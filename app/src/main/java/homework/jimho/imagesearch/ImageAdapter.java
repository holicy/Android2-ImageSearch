package homework.jimho.imagesearch;

import android.content.Context;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.List;

public class ImageAdapter extends ArrayAdapter<ImageItem>
{

    public ImageAdapter (Context context, List<ImageItem> images)
    {
        super(context, android.R.layout.simple_list_item_1, images);
    }

    @Override
    public View getView (int position, View convert_view, ViewGroup parent)
    {
        if (convert_view == null) {
            convert_view = LayoutInflater.from(getContext()).inflate(R.layout.image_item, parent, false);
        }

        ImageItem item = getItem(position);

        ImageView image_view = (ImageView) convert_view.findViewById(R.id.ivImage);
        image_view.setImageResource(0);
        Picasso.with(getContext()).load(item.url).into(image_view);

        TextView title = (TextView) convert_view.findViewById(R.id.tvImageTitle);
        title.setText(Html.fromHtml(item.title));

        return convert_view;
    }

}
