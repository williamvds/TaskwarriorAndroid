package kvj.taskw.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.ShareActionProvider;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.jetbrains.annotations.NotNull;
import org.kvj.bravo7.form.FormController;
import org.kvj.bravo7.form.impl.ViewFinder;
import org.kvj.bravo7.form.impl.bundle.ListStringBundleAdapter;
import org.kvj.bravo7.form.impl.bundle.StringBundleAdapter;
import org.kvj.bravo7.form.impl.widget.TextViewCharSequenceAdapter;
import org.kvj.bravo7.form.impl.widget.TransientAdapter;
import org.kvj.bravo7.log.Logger;
import org.kvj.bravo7.util.Tasks;

import java.util.ArrayList;
import java.util.List;

import kvj.taskw.App;
import kvj.taskw.R;
import kvj.taskw.data.AccountController;
import kvj.taskw.data.Controller;
import kvj.taskw.ui.AppActivity;

/**
 * Created by vorobyev on 12/1/15.
 */
public class RunActivity extends AppActivity {

    FormController form = new FormController(new ViewFinder.ActivityViewFinder(this));
    Controller controller = App.controller();
    Logger logger = Logger.forInstance(this);
    private AccountController ac = null;
    private RunAdapter adapter = null;
    private kvj.taskw.data.AccountController.TaskListener progressListener = null;
    private ShareActionProvider mShareActionProvider = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_run);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        RecyclerView list = (RecyclerView) findViewById(R.id.run_output);
        list.setLayoutManager(new LinearLayoutManager(this));
        setSupportActionBar(toolbar);
        final EditText input = findViewById(R.id.run_command);
        input.setOnKeyListener(new View.OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (keyCode != KeyEvent.KEYCODE_ENTER) return false;

                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    run();
                }

                return true;
            }
        });
        form.add(new TransientAdapter<>(new StringBundleAdapter(), null), App.KEY_ACCOUNT);
        form.add(new TransientAdapter<>(new ListStringBundleAdapter(), null), App.KEY_RUN_OUTPUT);
        form.add(new TextViewCharSequenceAdapter(R.id.run_command, null), App.KEY_RUN_COMMAND);
        form.load(this, savedInstanceState);
        progressListener = MainActivity
            .setupProgressListener(this, (ProgressBar) findViewById(R.id.progress));
        ac = controller.accountController(form);
        if (null == ac) {
            controller.messageShort("Invalid arguments");
            finish();
            return;
        }
        adapter = new RunAdapter(form.getValue(App.KEY_RUN_OUTPUT, ArrayList.class));
        list.setAdapter(adapter);
        toolbar.setSubtitle(ac.name());
    }

    private void shareAll() {
        CharSequence text = adapter.allText();
        if (TextUtils.isEmpty(text)) { // Error
            controller.messageShort("Nothing to share");
            return;
        }
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, text);
        sendIntent.setType("text/plain");
        if (null != mShareActionProvider) {
            logger.d("Share provider set");
            mShareActionProvider.setShareIntent(sendIntent);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_run, menu);

        // Locate MenuItem with ShareActionProvider
        MenuItem item = menu.findItem(R.id.menu_tb_run_share);

        // Fetch and store ShareActionProvider
        mShareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(item);

        // Return true to display menu
        return true;

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_tb_run_run:
                run();
                return true;
            case R.id.menu_tb_run_copy:
                copyAll();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void copyAll() {
        CharSequence text = adapter.allText();
        if (TextUtils.isEmpty(text)) { // Error
            controller.messageShort("Nothing to copy");
            return;
        }
        controller.copyToClipboard(text);
    }

    private static class RunTask extends StaticAsyncTask<RunActivity, Void, Void, Boolean> {
        private String input;
        private AccountController.ListAggregator out, err;

        RunTask(RunActivity activity, String input, AccountController.ListAggregator out, AccountController.ListAggregator err) {
            super(activity);
            this.input = input;
            this.out = out;
            this.err = err;
        }

        @Override
        protected Boolean background(RunActivity activity, Void... params) {
            int result = activity.ac.taskCustom(input, out, err);
            return 0 == result;
        }

        @Override
        protected void finish(RunActivity activity, Boolean result) {
            out.data().addAll(err.data());
            activity.adapter.addAll(out.data());
            activity.shareAll();
        }
    }

    private void run() {
        final String input = form.getValue(App.KEY_RUN_COMMAND);
        if (TextUtils.isEmpty(input)) {
            controller.messageShort("Input is empty");
            return;
        }
        adapter.clear();
        final AccountController.ListAggregator out = new AccountController.ListAggregator();
        final AccountController.ListAggregator err = new AccountController.ListAggregator();
        new RunTask(this, input, out, err).execute();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (null != adapter) {
            form.setValue(App.KEY_RUN_OUTPUT, adapter.data);
        }
        form.save(outState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        ac.listeners().add(progressListener, true);
    }

    @Override
    protected void onPause() {
        super.onPause();
        ac.listeners().remove(progressListener);
    }

    class RunAdapter extends RecyclerView.Adapter<RunAdapter.RunAdapterItem> {

        ArrayList<String> data = new ArrayList<>();

        public RunAdapter(ArrayList<String> data) {
            if (null != data) {
                this.data.addAll(data);
            }
        }

        @NotNull
        @Override
        public RunAdapterItem onCreateViewHolder(@NotNull ViewGroup parent, int viewType) {
            return new RunAdapterItem(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_run_output, parent, false));
        }

        @Override
        public void onBindViewHolder(@NotNull RunAdapterItem holder, int position) {
            holder.text.setText(data.get(position));
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        void clear() {
            notifyItemRangeRemoved(0, data.size());
            data.clear();
        }

        public synchronized void addAll(List<String> data) {
            int from = getItemCount();
            this.data.addAll(data);
            notifyItemRangeInserted(from, data.size());
        }

        public CharSequence allText() {
            StringBuilder sb = new StringBuilder();
            for (String line : data) { // Copy to
                if (sb.length() > 0) {
                    sb.append('\n');
                }
                sb.append(line);
            }
            return sb;
        }

        class RunAdapterItem extends RecyclerView.ViewHolder implements View.OnLongClickListener {

            private final TextView text;

            public RunAdapterItem(View itemView) {
                super(itemView);
                itemView.setOnLongClickListener(this);
                this.text = (TextView) itemView.findViewById(R.id.run_item_text);
            }

            @Override
            public boolean onLongClick(View v) {
                controller.copyToClipboard(data.get(getAdapterPosition()));
                return true;
            }
        }
    }

}
