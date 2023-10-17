package com.example.multiplefileuploads;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class imagesAdapter extends ArrayAdapter<imagesModel> {
    Context context;
    Activity activity;
    ArrayList<imagesModel> objects;

    public imagesAdapter(@NonNull Context context, ArrayList<imagesModel> objects, Activity activity) {
        super(context, R.layout.image_preview_layout_fvo, objects);
        this.objects = objects;
        this.context = context;
        this.activity=activity;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        imagesModel item = objects.get(position);

        ViewHolder viewHolder;

        if (convertView == null) {
            // If there's no view to re-use, inflate a brand new view for row
            viewHolder = new ViewHolder();
            LayoutInflater inflater = LayoutInflater.from(getContext());
            convertView = inflater.inflate(R.layout.image_preview_layout_fvo, parent, false);
            // Initializing the required variables.
            viewHolder.deleteImage=convertView.findViewById(R.id.deleteImage);
            viewHolder.hospitalPhotoIV=convertView.findViewById(R.id.hospitalPhotoIV);
            viewHolder.imageNumberTV=convertView.findViewById(R.id.imageNumberTV);


            // Cache the viewHolder object inside the fresh view
            convertView.setTag(viewHolder);
        } else {
            // View is being recycled, retrieve the viewHolder object from tag
            viewHolder = (ViewHolder) convertView.getTag();
        }

        try{
            try {
                /*Picasso.get().load(Uri.parse(item.getImagesUrl()))
                        .placeholder(R.drawable.album)
                        .error(R.drawable.album)
                        .into(viewHolder.hospitalPhotoIV);*/
                Picasso.get().load(item.getFile())
                        .error(R.drawable.cancel)
                        .into(viewHolder.hospitalPhotoIV);
                System.out.println(item.getImageNum());
                viewHolder.imageNumberTV.setText(String.valueOf(item.getImageNum()));
                viewHolder.deleteImage.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        objects.remove(position);
                        notifyDataSetChanged();
                    }
                });


            } catch (Exception e) {
                e.printStackTrace();
            }
        }catch (Exception e){
            e.printStackTrace();
        }


        return convertView;
    }

    public static class ViewHolder {
        // Declaring the variables.
        ImageView deleteImage,hospitalPhotoIV;
        TextView imageNumberTV;

    }
}
