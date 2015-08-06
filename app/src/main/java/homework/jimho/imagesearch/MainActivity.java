package homework.jimho.imagesearch;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;

import com.etsy.android.grid.StaggeredGridView;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import com.jayway.jsonpath.ReadContext;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.TextHttpResponseHandler;

import org.apache.http.Header;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends ActionBarActivity {

    private List<ImageItem> images;
    private ArrayAdapter<ImageItem> list_adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setupViews();
    }

    private EditText query_text;
    private StaggeredGridView images_grid_view;

    private void setupViews()
    {
        images = new ArrayList<>();
        list_adapter = new ImageAdapter(getBaseContext(), images);

        images_grid_view = (StaggeredGridView) findViewById(R.id.gvImages);

        images_grid_view.setAdapter(list_adapter);
        images_grid_view.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(MainActivity.this, ImageDisplayActivity.class);

                ImageItem image_item = images.get(position);

                intent.putExtra("item", image_item);

                startActivity(intent);
            }

        });
        images_grid_view.setOnScrollListener(new AbsListView.OnScrollListener() {

            // The minimum amount of items to have below your current scroll position
            // before loading more.
            private int visibleThreshold = 3;
            // The current offset index of data you have loaded
            private int currentPage = 0;
            // The total number of items in the dataset after the last load
            private int previousTotalItemCount = 0;
            // True if we are still waiting for the last set of data to load.
            private boolean loading = true;
            // Sets the starting page index
            private int startingPageIndex = 0;

            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {}

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                // If the total item count is zero and the previous isn't, assume the
                // list is invalidated and should be reset back to initial state
                if (totalItemCount < previousTotalItemCount) {
                    this.currentPage = this.startingPageIndex;
                    this.previousTotalItemCount = totalItemCount;
                    if (totalItemCount == 0) { this.loading = true; }
                }
                // If it’s still loading, we check to see if the dataset count has
                // changed, if so we conclude it has finished loading and update the current page
                // number and total item count.
                if (loading && (totalItemCount > previousTotalItemCount)) {
                    loading = false;
                    previousTotalItemCount = totalItemCount;
                    currentPage++;
                }

                // If it isn’t currently loading, we check to see if we have breached
                // the visibleThreshold and need to reload more data.
                // If we do need to reload some more data, we execute onLoadMore to fetch the data.
                if (!loading && (totalItemCount - visibleItemCount)<=(firstVisibleItem + visibleThreshold)) {
                    onLoadMore(currentPage + 1, totalItemCount);
                    loading = true;
                }
            }

            public void onLoadMore(int page, int totalItemsCount)
            {
                AsyncHttpClient http = new AsyncHttpClient();

                http.get(getSearchURL(page), new TextHttpResponseHandler() {

                    @Override
                    public void onSuccess(int statusCode, Header[] headers, String responseString) {
                        List<Object> search_items;
                        try {
                            search_items = JsonPath.read(responseString, "$.responseData.results");
                        } catch (PathNotFoundException e) {
                            Log.i("DEBUG", e.getMessage());
                            return;
                        }

                        for (Object item: search_items) {
                            ReadContext item_cx = JsonPath.parse(item);

                            ImageItem image_item = new ImageItem();
                            try {
                                image_item.title = item_cx.read("$.title");
                                image_item.origin_url = item_cx.read("$.url");
                                image_item.url   = item_cx.read("$.tbUrl");
                                image_item.height = Integer.parseInt((String) item_cx.read("$.tbHeight"));
                                image_item.width  = Integer.parseInt((String) item_cx.read("$.tbWidth"));
                            } catch (Exception e) {
                                Log.i("DEBUG", e.getMessage());
                                continue;
                            }
                            images.add(image_item);
                            Log.i("DEBUG", String.format("%s %d %d", image_item.title, image_item.width, image_item.height));
                        }
                        list_adapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                        Log.i("DEBUG", throwable.getMessage());
                    }

                });
            }

        });
    }

    private final String image_search_api_format = "https://ajax.googleapis.com/ajax/services/search/images?v=1.0&q=%s&rsz=8&start=%d";

    protected String getSearchURL (int page)
    {
        String query = query_text.getText().toString(),
               url = String.format(image_search_api_format, query, page * 8);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

        String site  = preferences.getString("siteFilter", ""),
               type  = preferences.getString("imageType", ""),
               color = preferences.getString("imageColor", ""),
               size  = preferences.getString("imageSize", "");

        if (!site.equals("")) {
            url += "&as_sitesearch=" + site;
        }
        if (!type.equals("")) {
            url += "&imgtype=" + type;
        }
        if (!color.equals("")) {
            url += "&imgcolor=" + color;
        }
        if (!size.equals("")) {
            url += "&imgsz=" + size;
        }

        Log.i("DEBUG", "URL:" + url);

        return url;
    }

    @Override
    public boolean onCreateOptionsMenu (Menu menu)
    {
        getMenuInflater().inflate(R.menu.menu_main, menu);

        View view = menu.findItem(R.id.action_search).getActionView();

        query_text = (EditText) view.findViewById(R.id.tvQuery);
        Button search_button = (Button) view.findViewById(R.id.bvSearch);

        search_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AsyncHttpClient http = new AsyncHttpClient();

                http.get(getSearchURL(0), new TextHttpResponseHandler() {

                    @Override
                    public void onSuccess(int statusCode, Header[] headers, String responseString) {
                        List<Object> search_items;
                        try {
                            search_items = JsonPath.read(responseString, "$.responseData.results");
                        } catch (PathNotFoundException e) {
                            Log.d("DEBUG", e.getMessage());
                            return;
                        }

                        list_adapter.clear();
                        for (Object item: search_items) {
                            ReadContext item_cx = JsonPath.parse(item);

                            ImageItem image_item = new ImageItem();
                            try {
                                image_item.title = item_cx.read("$.title");
                                image_item.origin_url = item_cx.read("$.url");
                                image_item.url   = item_cx.read("$.tbUrl");
                                image_item.height = Integer.parseInt((String) item_cx.read("$.tbHeight"));
                                image_item.width  = Integer.parseInt((String) item_cx.read("$.tbWidth"));
                            } catch (Exception e) {
                                Log.d("DEBUG", e.getMessage());
                                continue;
                            }
                            images.add(image_item);
                            Log.i("DEBUG", String.format("%s %d %d", image_item.title, image_item.width, image_item.height));
                        }
                        list_adapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                        Log.d("DEBUG", throwable.getMessage());
                    }

                });
            }
        });

        return true;
    }

    @Override
    public boolean onOptionsItemSelected (MenuItem item) {
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_config) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
