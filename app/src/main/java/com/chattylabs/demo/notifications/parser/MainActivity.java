package com.chattylabs.demo.notifications.parser;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.transition.TransitionManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.chattylabs.sdk.android.notifications.NotificationData;
import com.chattylabs.sdk.android.notifications.NotificationItem;
import com.chattylabs.sdk.android.notifications.NotificationMessage;
import com.chattylabs.sdk.android.notifications.NotificationParserComponent;

import net.hockeyapp.android.CrashManager;
import net.hockeyapp.android.UpdateManager;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.android.support.DaggerAppCompatActivity;

public class MainActivity extends DaggerAppCompatActivity {

    private Button mLoadButton;
    private Button mLaunchButton;
    private TextView mExecutionText;
    private MessageAdapter mMessageAdapter;
    private RecyclerView recycler;
    private ProgressBar progress;

    @Inject NotificationParserComponent notificationParserComponent;

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || intent.getAction() == null || intent.getExtras() == null) return;
            String action = intent.getAction();
            Bundle extras = intent.getExtras();
            switch (action) {
                case NotificationParserComponent.ACTION_RETRIEVE_CURRENT:
                case NotificationParserComponent.ACTION_POST:
                    NotificationItem item = notificationParserComponent.extract(intent);
                    updateList(item);
                    break;

                case NotificationParserComponent.ACTION_LOG:
                    String message = extras.getString(NotificationParserComponent.EXTRA_REPORT_MESSAGE);
                    mExecutionText.setText(String.format("%s\n%s", mExecutionText.getText().toString(), message));
                    break;

                case NotificationParserComponent.ACTION_ERROR:
                    progress.setVisibility(View.GONE);
                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle("Unknown error")
                            .setMessage("Please, Switch off/on the Service again.")
                            .show();
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // If not using Dagger injection you can still get the component from the static method
        //notificationParserComponent = NotificationParserModule.provideNotificationParserComponent(new ILoggerImpl());

        // Makes sure the NotificationListener component is still enabled
        notificationParserComponent.enableComponent(this);

        // Launches Notification Access device settings
        mLaunchButton = findViewById(R.id.launch_settings);
        mLaunchButton.setOnClickListener(v -> notificationParserComponent.launchSettings(this));

        // Retrieves and shows the current list of active notifications
        mLoadButton = findViewById(R.id.load_actives);
        mLoadButton.setOnClickListener(v -> {
            if (notificationParserComponent.isEnabled(this)) {
                progress.setVisibility(View.VISIBLE);
                mMessageAdapter.setData(new ArrayList<>());
                notificationParserComponent.retrieveActiveNotifications(this);
            }
        });

        int minSize = getResources().getDimensionPixelSize(R.dimen.execution_text_min_size);
        int maxSize = getResources().getDimensionPixelSize(R.dimen.execution_text_max_size);

        // Shows the executions logs
        mExecutionText = findViewById(R.id.execution);
        mExecutionText.setHeight(minSize);
        mExecutionText.setOnClickListener(v -> {
            TransitionManager.beginDelayedTransition(findViewById(R.id.root));
            ((TextView)v).setHeight(v.getHeight() == minSize ?  maxSize : minSize);
        });
        progress = findViewById(R.id.progressBar);
        progress.setVisibility(View.GONE);
        mExecutionText.setMovementMethod(new ScrollingMovementMethod());

        // Initialize the RecyclerView and its Adapter
        recycler = findViewById(R.id.list);
        mMessageAdapter = new MessageAdapter(this, listener);
        mMessageAdapter.setData(new ArrayList<>());
        final LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setStackFromEnd(true);
        mMessageAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeChanged(int positionStart, int itemCount) {
                super.onItemRangeChanged(positionStart, itemCount);
                linearLayoutManager.smoothScrollToPosition(recycler, null, mMessageAdapter.getItemCount());
            }
        });
        //linearLayoutManager.setReverseLayout(true);
        recycler.setLayoutManager(linearLayoutManager);
        recycler.setAdapter(mMessageAdapter);

        // HokeyApp Events
        UpdateManager.register(this);
        //FeedbackManager.setActivityForScreenshot(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        CrashManager.register(this);
        checkEnabled();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        UpdateManager.unregister();
    }

    @Override
    public void onStart() {
        super.onStart();
        IntentFilter filter = new IntentFilter();
        filter.addAction(NotificationParserComponent.ACTION_ERROR);
        filter.addAction(NotificationParserComponent.ACTION_LOG);
        filter.addAction(NotificationParserComponent.ACTION_POST);
        filter.addAction(NotificationParserComponent.ACTION_RETRIEVE_CURRENT);
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, filter);
    }

    @Override
    protected void onStop() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
        super.onStop();
    }

    private MessageAdapter.OnItemClickListener listener = item -> {
        String text = "package: " + item.getPackageName() + "\n\n" +
                      "time: " + NotificationItem.getDatetime(item.getTimestamp()) + "\n\n";
        if (item.getType() == NotificationItem.MESSAGE) {
            NotificationMessage message = (NotificationMessage) item;
            text += "type: MESSAGE (Already developed, No need to check!)\n\n" +
                    "sender: " + message.getSender() + "\n\n" +
                    "message: " + message.getText() + "\n"
            ;
        }
        else if (item.getType() == NotificationItem.DATA) {
            NotificationData data = (NotificationData) item;
            text += "type: DATA\n\n" +
                    "when: " + NotificationItem.getDatetime(data.when) + "\n\n" +
                    "category: " + data.category + "\n\n" +
                    "group: " + data.group + "\n\n" +
                    "sbnKey: " + data.sbnKey + "\n\n" +
                    "sbnTag: " + data.sbnTag + "\n\n" +
                    "settingsText: " + data.settingsText + "\n\n" +
                    "shortcutId: " + data.shortcutId + "\n\n" +
                    "sortKey: " + data.sortKey + "\n\n" +
                    "tickerText: " + data.tickerText + "\n\n" +
                    "number: " + data.number + "\n\n" +
                    "sbnId: " + data.sbnId + "\n\n" +
                    "sbnIsGroup: " + data.sbnIsGroup + "\n\n" +
                    "---------- EXTRAS --------" + "\n\n" +
                    data.extras + "\n\n" +
                    "---------- ACTIONS --------" + "\n\n" +
                    data.actionsString + "\n"
            ;
        }

        new AlertDialog.Builder(MainActivity.this)
                .setTitle(item.getPackageName())
                .setMessage(text).show();
    };

    private String join(String[] strings) {
        String space = "\n       - ";
        if (strings != null) {
            return space + TextUtils.join(space, strings);
        }
        return null;
    }

    private void updateList(NotificationItem item) {
        progress.setVisibility(View.GONE);
        if (item != null) {
            mMessageAdapter.update(item);
            mExecutionText.setText(String.format("%s%nUpdating with item %s",
                    mExecutionText.getText().toString(), item.getPackageName()));
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        checkEnabled();
        if (!notificationParserComponent.isEnabled(this)) {
            progress.setVisibility(View.GONE);
        }
    }

    private void checkEnabled() {
        if (notificationParserComponent.isEnabled(this)) {
            mLoadButton.setVisibility(View.VISIBLE);
            mLaunchButton.setText(R.string.launch_to_disable);
            mExecutionText.setText(String.format("%s%n%s",
                    mExecutionText.getText().toString(), getString(R.string.waiting_for_content)));
        }
        else {
            mLoadButton.setVisibility(View.GONE);
            mLaunchButton.setText(R.string.launch_to_enable);
            mExecutionText.setText(String.format("%s%n%s",
                    mExecutionText.getText().toString(), getString(R.string.nothing_to_see)));
        }
    }

    private static class Holder extends RecyclerView.ViewHolder {
        private final TextView packageName;
        private final TextView time;
        private final TextView ticketText;

        Holder(View itemView) {
            super(itemView);
            packageName = itemView.findViewById(R.id.packageName);
            time = itemView.findViewById(R.id.time);
            ticketText = itemView.findViewById(R.id.ticketText);
        }
    }

    public static class MessageAdapter extends RecyclerView.Adapter<Holder> {

        public interface OnItemClickListener {
            void onItemClick(NotificationItem item);
        }

        private final Context mContext;
        private final OnItemClickListener listener;
        private List<NotificationItem> items;

        public MessageAdapter(Context context, OnItemClickListener listener) {
            mContext = context;
            this.listener = listener;
            setHasStableIds(true);
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item,
                                                                               parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            NotificationItem item = items.get(position);

            holder.itemView.setOnClickListener(v -> listener.onItemClick(item));

            holder.packageName.setText(item.getPackageName());
            holder.time.setText(NotificationItem.getDatetime(item.getTimestamp()));

            if (item.getType() == NotificationItem.MESSAGE) {
                NotificationMessage message = (NotificationMessage) item;
                holder.ticketText.setText(String.format(
                        "Type: MESSAGE (Already developed, No need to check!)%n%s: %s",
                        message.getSender(), message.getText()));
            }
            else if (item.getType() == NotificationItem.DATA) {
                NotificationData data = (NotificationData) item;
                String text = "...perhaps a custom view?";
                if (!TextUtils.isEmpty(data.titleText) || !TextUtils.isEmpty(data.messageText)) {
                    text = "titleText - " + data.titleText;
                    text += "\nmessageText - " + data.messageText;
                } else if (!TextUtils.isEmpty(data.tickerText)) {
                    text = "tickerText - " + data.tickerText;
                }
                holder.ticketText.setText(text);
            }
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public int getItemCount() {
            return items == null ? 0 : items.size();
        }

        public void update(NotificationItem item) {
            if (items != null) {
                synchronized (items) {
                    items.add(item);
                    notifyItemChanged(items.size() - 1);
                }
            }
        }

        public void setData(List<? extends NotificationItem> items) {
            //noinspection unchecked
            this.items = (List<NotificationItem>) items;
            notifyDataSetChanged();
        }
    }
}
