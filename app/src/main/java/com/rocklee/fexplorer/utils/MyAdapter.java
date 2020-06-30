package com.rocklee.fexplorer.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.rocklee.fexplorer.R;
import com.rocklee.fexplorer.activities.FExplorerActivity;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MyAdapter extends BaseAdapter {
    private static final String TAG = "LC_Adapter";
    private LayoutInflater inflater;
    private Bitmap directory,file;
    private ArrayList<String> names = new ArrayList<>();
    private ArrayList<String> paths = new ArrayList<>();
    private ArrayList<String> uri = new ArrayList<>();
    private ArrayList<Integer> id = new ArrayList<>();
    private List<VideoContainer> videoList = new ArrayList<>();

    public MyAdapter(Context context,ArrayList<String> na,ArrayList<String> pa){
        names = na;
        paths = pa;
        directory = BitmapFactory.decodeResource(context.getResources(), R.drawable.folder);
        file = BitmapFactory.decodeResource(context.getResources(),R.drawable.file);
        directory = small(directory,0.2f);
        file = small(file,0.2f);
        inflater = LayoutInflater.from(context);
    }

    public MyAdapter(Context context, List<VideoContainer> list){
        videoList = list;
        int i = 0;
        for(i = 0; i < videoList.size(); i++) {
            names.add(videoList.get(i).name);
            uri.add(videoList.get(i).uri);
            id.add(videoList.get(i).id);
        }
        directory = BitmapFactory.decodeResource(context.getResources(), R.drawable.folder);
        file = BitmapFactory.decodeResource(context.getResources(),R.drawable.file);
        directory = small(directory,0.2f);
        file = small(file,0.2f);
        inflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return names.size();
    }

    @Override
    public Object getItem(int position) {
        return names.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (null == convertView){
            convertView = inflater.inflate(R.layout.file, null);
            holder = new ViewHolder();
            holder.text = (TextView)convertView.findViewById(R.id.textView);
            holder.image = (ImageView)convertView.findViewById(R.id.imageView);

            convertView.setTag(holder);
        }
        else {
            holder = (ViewHolder)convertView.getTag();
        }
        holder.text.setText(names.get(position));
        holder.image.setImageBitmap(file);
//        File f = new File(paths.get(position).toString());
//        if (names.get(position).equals("@1")){
//            holder.text.setText("/");
//            holder.image.setImageBitmap(directory);
//        }
//        else if (names.get(position).equals("@2")){
//            holder.text.setText("..");
//            holder.image.setImageBitmap(directory);
//        }
//        else{
//            holder.text.setText(f.getName());
//            if (f.isDirectory()){
//                holder.image.setImageBitmap(directory);
//            }
//            else if (f.isFile()){
//                holder.image.setImageBitmap(file);
//            }
//            else{
//                System.out.println(f.getName());
//            }
//        }
        return convertView;
    }
    private class ViewHolder{
        private TextView text;
        private ImageView image;
    }
    private Bitmap small(Bitmap map,float num){
        Matrix matrix = new Matrix();
        matrix.postScale(num, num);
        return Bitmap.createBitmap(map,0,0,map.getWidth(),map.getHeight(),matrix,true);
    }
}
