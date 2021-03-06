/*
 * Copyright (c) 2014, Kinvey, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.kinvey.sample.contentviewr;

import static com.kinvey.sample.contentviewr.Contentviewr.CONTENT_COLLECTION;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.kinvey.android.AsyncAppData;
import com.kinvey.android.callback.KinveyListCallback;
import com.kinvey.android.offline.SqlLiteOfflineStore;
import com.kinvey.java.Query;
import com.kinvey.java.offline.OfflinePolicy;
import com.kinvey.sample.contentviewr.core.ContentFragment;
import com.kinvey.sample.contentviewr.model.ContentItem;
import com.kinvey.sample.contentviewr.model.ContentType;
import com.kinvey.sample.contentviewr.windows.Viewer;
import com.kinvey.sample.contentviewr.windows.WindowFactory;

/**
 * @author edwardf
 */
public class ContentListFragment extends ContentFragment implements AdapterView.OnItemClickListener {

    protected ListView contentList;
    protected ContentListAdapter adapter;
    protected ContentType type;
    protected LinearLayout loading;
    private TextView empty;

    protected List<ContentItem> content;

    public static ContentListFragment newInstance(ContentType type){
        ContentListFragment ret = new ContentListFragment();
        ret.setType(type);
        return ret;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null){
            type = savedInstanceState.getParcelable("type");
        }
        setHasOptionsMenu(true);
        getSherlockActivity().invalidateOptionsMenu();

    }

    @Override
    public void onSaveInstanceState(Bundle saved){
        super.onSaveInstanceState(saved);
        saved.putParcelable("type", type);
    }

    @Override
    public void onResume(){
        super.onResume();
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup group, Bundle saved) {
        Log.i(Contentviewr.TAG, "content list got oncreateview");
        View v = inflater.inflate(R.layout.fragment_content_list, group, false);
        bindViews(v);
        return v;
    }

    @Override
    public int getViewID() {
        return R.layout.fragment_content_list;
    }


    public void bindViews(View v){
        contentList = (ListView) v.findViewById(R.id.content_list);
        loading = (LinearLayout) v.findViewById(R.id.content_loadingbox);
        empty = (TextView) v.findViewById(R.id.content_none);
        empty.setVisibility(View.GONE);

        refresh();
        contentList.setOnItemClickListener(this);

    }


    public void refresh(){
        if (loading == null || getContentViewr() == null || getClient() == null){
            return;
        }


        loading.setVisibility(View.VISIBLE);
        Query q = new Query()
                .equals("type", type.getName())
                .equals("target", getContentViewr().getSelectedTarget())
                .equals("groups", getClient().user().get("group")) ;

        AsyncAppData<ContentItem> app = getClient().appData(CONTENT_COLLECTION, ContentItem.class);
        app.setOffline(OfflinePolicy.LOCAL_FIRST, new SqlLiteOfflineStore(getSherlockActivity().getApplicationContext()));
        app.get(q, new KinveyListCallback<ContentItem>() {
            @Override
            public void onSuccess(ContentItem[] result) {
                if (getSherlockActivity() == null || getClient() == null) {
                    return;
                }
                loading.setVisibility(View.GONE);
                if (result != null){
                	content = Arrays.asList(result);
                }else{
                	content = new ArrayList<ContentItem>();
                }

                if (content.size() == 0){
                    empty.setVisibility(View.VISIBLE);
                    return;
                }else{
                    empty.setVisibility(View.GONE);
                }


                adapter = new ContentListAdapter(getSherlockActivity(), content,
                        (LayoutInflater) getSherlockActivity().getSystemService(
                                Activity.LAYOUT_INFLATER_SERVICE));

                contentList.setAdapter(adapter);

                //Lazy load images
                for (ContentItem c : content) {
                    if (c != null && getClient() != null){
                        c.loadThumbnail(getClient(), adapter);
                    }
                }
            }

            @Override
            public void onFailure(Throwable error) {
                if (getSherlockActivity() == null) {
                    return;
                }
                loading.setVisibility(View.GONE);
                Util.Error(ContentListFragment.this, error);
            }
        });
    }

    public String getTitle(){
        return type.getDisplayName();
    }
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        ContentItem item = adapter.getItem(position);

        Viewer viewer = new WindowFactory().getViewer(type.getWindowstyle());
        viewer.loadContent(adapter.getItem(position));
        showWindow(viewer);

    }

    public ContentType getType() {
        return type;
    }

    public void setType(ContentType type) {
        this.type = type;
    }

}
