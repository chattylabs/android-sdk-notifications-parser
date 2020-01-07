package chattylabs.notifications.demo;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.transition.TransitionManager;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import chattylabs.android.commons.internal.ILoggerImpl;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.text.HtmlCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.flexbox.FlexboxLayout;

import chattylabs.notifications.NotificationAction;
import chattylabs.notifications.NotificationData;
import chattylabs.notifications.NotificationGeneric;
import chattylabs.notifications.NotificationItem;
import chattylabs.notifications.NotificationMessage;
import chattylabs.notifications.NotificationParser;
import chattylabs.notifications.NotificationParserProvider;
import chattylabs.notifications.NotificationService;

public class MainActivity extends AppCompatActivity {

    private Button mLoadButton;
    private Button mLaunchButton;
    private TextView mExecutionText;
    private MessageAdapter mMessageAdapter;
    private RecyclerView recycler;
    private ProgressBar progress;

    NotificationParser parserComponent;

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || intent.getAction() == null || intent.getExtras() == null) return;
            String action = intent.getAction();
            Bundle extras = intent.getExtras();
            switch (action) {
                case NotificationParser.ACTION_POST:
                    NotificationItem item = parserComponent.extract(intent);
                    updateList(item);
                    break;

                case NotificationParser.ACTION_LOG:
                    String message = extras.getString(NotificationParser.EXTRA_REPORT_MESSAGE);
                    mExecutionText.setText(String.format("%s\n%s", mExecutionText.getText().toString(), message));
                    break;

                case NotificationParser.ACTION_ERROR:
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
        parserComponent = NotificationParserProvider.provide();
        NotificationService.logger = new ILoggerImpl();

        // Launches Notification Access device settings
        mLaunchButton = findViewById(R.id.launch_settings);
        mLaunchButton.setOnClickListener(v -> parserComponent.launchSettings(this));

        parserComponent.enableComponentSilent(this);

        // Retrieves and shows the current list of active chattylabs.notifications
        mLoadButton = findViewById(R.id.load_actives);
        mLoadButton.setOnClickListener(v -> {
            if (parserComponent.isEnabled(this)) {
                progress.setVisibility(View.VISIBLE);
                mMessageAdapter.setData(new ArrayList<>());
                mMessageAdapter.notifyDataSetChanged();
                parserComponent.fetchActiveNotifications(this, data -> {
                    runOnUiThread(() -> {
                        for (NotificationData item : data) {
                            updateList(item);
                        }
                    });
                });
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 0);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        checkEnabled();
    }

    @Override
    public void onStart() {
        super.onStart();
        IntentFilter filter = new IntentFilter();
        filter.addAction(NotificationParser.ACTION_ERROR);
        filter.addAction(NotificationParser.ACTION_LOG);
        filter.addAction(NotificationParser.ACTION_POST);
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, filter);
    }

    @Override
    protected void onStop() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
        super.onStop();
    }

    private MessageAdapter.OnItemClickListener listener = item -> {
        String text = "<b>package:</b> " + item.getPackageName() + "<br/><br/>" +
                      "<b>post time:</b> " + NotificationItem.getDatetime(item.getPostTime()) + "<br/><br/>";
        if (item.getType() == NotificationItem.MESSAGE) {
            NotificationMessage message = (NotificationMessage) item;
            text += "<b>type:</b> MESSAGE (Already developed, No need to check!)<br/><br/>" +
                    "<b>sender:</b> " + message.getSender() + "<br/><br/>" +
                    "<b>message:</b> " + message.getText() + "<br/>"
            ;
        }
        else if (item.getType() == NotificationItem.DATA) {
            NotificationData data = (NotificationData) item;
            text += "<b>type:</b> DATA<br/>" +
                    "<b>when:</b> " + NotificationItem.getDatetime(data.getItemTime()) + "<br/>" +
                    "<b>category:</b> " + data.getCategory() + "<br/>" +
                    "<b>channelId:</b> " + data.getChannel() + "<br/>" +
                    "<b>group:</b> " + data.group + "<br/>" +
                    "<b>sbnKey:</b> " + data.sbnKey + "<br/>" +
                    "<b>sbnTag:</b> " + data.sbnTag + "<br/>" +
                    "<b>settingsText:</b> " + data.settingsText + "<br/>" +
                    "<b>shortcutId:</b> " + data.shortcutId + "<br/>" +
                    "<b>sortKey:</b> " + data.sortKey + "<br/>" +
                    "<b>tickerText:</b> " + data.tickerText + "<br/>" +
                    "<b>number:</b> " + data.number + "<br/>" +
                    "<b>sbnId:</b> " + data.sbnId + "<br/>" +
                    "<b>sbnIsGroup:</b> " + data.sbnIsGroup + "<br/>" +
                    "<br/>EXTRAS<br/>" + data.extras + "<br/>" +
                    "<br/>ACTIONS<br/>" + data.actionsString + "<br/>"
            ;
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(item.getPackageName())
                .setMessage(HtmlCompat.fromHtml(text, HtmlCompat.FROM_HTML_MODE_LEGACY)).show();
        TextView v = ((TextView) dialog.findViewById(android.R.id.message));
        v.setTypeface(Typeface.MONOSPACE);
        v.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
    };

    private String join(String[] strings) {
        String space = "<br/>       - ";
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
        super.onActivityResult(requestCode, resultCode, data);
        checkEnabled();
        if (!parserComponent.isEnabled(this)) {
            progress.setVisibility(View.GONE);
        }
    }

    private void checkEnabled() {
        if (parserComponent.isEnabled(this)) {
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
        private final ImageView icon;
        private final TextView name;
        private final TextView packageName;
        private final TextView time;
        private final TextView ticketText;
        private final EditText sendText;
        private final Button sendButton;
        private final Spinner spinner;
        private final FlexboxLayout buttons;

        Holder(View itemView) {
            super(itemView);
            icon        = itemView.findViewById(R.id.icon);
            name        = itemView.findViewById(R.id.name);
            packageName = itemView.findViewById(R.id.packageName);
            time        = itemView.findViewById(R.id.time);
            ticketText  = itemView.findViewById(R.id.ticketText);
            sendText    = itemView.findViewById(R.id.sendText);
            sendButton  = itemView.findViewById(R.id.sendButton);
            spinner     = itemView.findViewById(R.id.spinner);
            buttons     = itemView.findViewById(R.id.buttons);
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

            holder.time.setText(NotificationItem.getDatetime(item.getItemTime()));

            holder.sendText.setVisibility(View.GONE);
            holder.sendButton.setVisibility(View.GONE);
            holder.spinner.setVisibility(View.GONE);
            holder.buttons.setVisibility(View.GONE);
            holder.buttons.removeAllViews();

            if (item.getType() == NotificationItem.MESSAGE) {
                holder.sendText.setVisibility(View.VISIBLE);
                holder.sendButton.setVisibility(View.VISIBLE);
                holder.spinner.setVisibility(View.VISIBLE);
                holder.packageName.setVisibility(View.VISIBLE);
                holder.packageName.setText(item.getPackageName());
                NotificationMessage message = (NotificationMessage) item;
                holder.name.setText(message.getSender());
                holder.icon.setVisibility(View.VISIBLE);
                holder.icon.setImageBitmap(message.getAvatar());
                holder.ticketText.setText(
                        HtmlCompat.fromHtml(String.format(
                        "<b>Type:</b> MESSAGE " +
                        "<br/><b>%s</b>: %s", message.getSender(), message.getText()), HtmlCompat.FROM_HTML_MODE_LEGACY));
                processActionsSpinner(holder, message);
            }
            else if (item.getType() == NotificationItem.GENERIC) {
                holder.packageName.setVisibility(View.VISIBLE);
                holder.buttons.setVisibility(View.VISIBLE);
                holder.packageName.setText(item.getPackageName());
                NotificationGeneric generic = (NotificationGeneric) item;
                holder.name.setText(generic.getApplicationName());
                holder.icon.setVisibility(View.VISIBLE);
                holder.icon.setImageBitmap(generic.getIcon());
                holder.ticketText.setText(
                    HtmlCompat.fromHtml(String.format(
                        "<b>Type:</b> GENERIC " +
                        "<br/><b>%s</b>: %s - %s", generic.getTitle(), generic.getText(), generic.getSubText()), HtmlCompat.FROM_HTML_MODE_LEGACY));
                processActionsButtons(holder, generic);
            }
            else if (item.getType() == NotificationItem.DATA) {
                holder.icon.setVisibility(View.GONE);
                holder.packageName.setVisibility(View.GONE);
                holder.name.setText(item.getPackageName());
                NotificationData data = (NotificationData) item;
                String text = "...perhaps a custom view?";
                if (!TextUtils.isEmpty(data.titleText) || !TextUtils.isEmpty(data.messageText)) {
                    text = "<b>titleText</b> - " + data.titleText;
                    text += "<br/><b>messageText</b> - " + data.messageText;
                } else if (!TextUtils.isEmpty(data.tickerText)) {
                    text = "<b>tickerText</b> - " + data.tickerText;
                }
                holder.ticketText.setText(HtmlCompat.fromHtml(text, HtmlCompat.FROM_HTML_MODE_LEGACY));
            }
        }

        private void processActionsSpinner(@NonNull Holder holder, NotificationItem item) {
            ArrayAdapter<String> ad = new ArrayAdapter<>(holder.itemView.getContext(), android.R.layout.simple_spinner_item);
            holder.spinner.setAdapter(ad);
            ArrayList<String> s = new ArrayList<>();
            for (NotificationAction a :item.getActions()) {
                s.add(a.getText());
            }
            ad.addAll(s.toArray(new String[s.size()]));
            holder.sendButton.setOnClickListener(v -> {
                for (NotificationAction a :item.getActions()) {
                    if (a.getText() == holder.spinner.getSelectedItem()) {
                        a.send(holder.itemView.getContext(),
                               holder.sendText.getText().toString(),
                               () -> {
                                   holder.sendText.setText(null);
                                   holder.sendText.setHint("Success");
                               },
                               () -> {
                                   holder.sendText.setText(null);
                                   holder.sendText.setHint("Error");
                               });
                    }
                }
            });
        }

        private void processActionsButtons(@NonNull Holder holder, NotificationItem item) {

            for (NotificationAction a :item.getActions()) {
                ViewGroup.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                                                                                    ViewGroup.LayoutParams.WRAP_CONTENT);
                Button button = new Button(holder.itemView.getContext());
                button.setLayoutParams(layoutParams);
                button.setText(a.getText());
                button.setEnabled(a.getActionIntent() != null);
                button.setOnClickListener(v -> {
                    button.setEnabled(false);
                    a.send(holder.itemView.getContext(),
                           null,
                           () -> {
                               button.setEnabled(true);
                           },
                           () -> {});
                });
                holder.buttons.addView(button);
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
