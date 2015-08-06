package homework.jimho.imagesearch;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;

public class ImageDisplayActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_display);

        ImageItem item = (ImageItem) getIntent().getSerializableExtra("item");

        ImageView view = (ImageView) findViewById(R.id.ivFullImage);

        Picasso.with(this).load(item.origin_url).into(view);
    }

}
