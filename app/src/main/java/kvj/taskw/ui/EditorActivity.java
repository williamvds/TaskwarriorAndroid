package kvj.taskw.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ProgressBar;

import kvj.taskw.data.UUIDBundleAdapter;
import org.kvj.bravo7.form.BundleAdapter;
import org.kvj.bravo7.form.FormController;
import org.kvj.bravo7.form.impl.ViewFinder;
import org.kvj.bravo7.form.impl.bundle.StringBundleAdapter;
import org.kvj.bravo7.form.impl.widget.TransientAdapter;
import org.kvj.bravo7.log.Logger;
import org.kvj.bravo7.util.DataUtil;
import org.kvj.bravo7.util.Tasks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import kvj.taskw.App;
import kvj.taskw.R;
import kvj.taskw.data.AccountController;
import kvj.taskw.data.Controller;
import kvj.taskw.ui.AppActivity;

/**
 * Created by kvorobyev on 11/21/15.
 */
public class EditorActivity extends AppActivity {

    private Toolbar toolbar = null;
    private Editor editor = null;
    private FormController form = new FormController(new ViewFinder.ActivityViewFinder(this));
    Controller controller = App.controller();
    Logger logger = Logger.forInstance(this);
    private List<String> priorities = null;
    private AccountController.TaskListener progressListener = null;
    private AccountController ac = null;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);
        toolbar = findViewById(R.id.toolbar);
        editor = (Editor) getSupportFragmentManager().findFragmentById(R.id.editor_editor);
        ProgressBar progressBar = findViewById(R.id.progress);
        setSupportActionBar(toolbar);
        logger.e("OnCreate", savedInstanceState);
        form.add(new TransientAdapter<>(new StringBundleAdapter(), null), App.KEY_ACCOUNT);
        form.add(new TransientAdapter<>(new UUIDBundleAdapter(), null), App.KEY_EDIT_UUID);
        form.add(new TransientAdapter<>(new BundleAdapter<Bundle>() {
            @Override
            public Bundle get(Bundle bundle, String name, Bundle def) {
                return bundle.getBundle(name);
            }

            @Override
            public void set(Bundle bundle, String name, Bundle value) {
                bundle.putBundle(name, value);
            }
        }, null).oneShot(), App.KEY_EDIT_DATA);
        form.add(new TransientAdapter<>(new BundleAdapter<ArrayList<String>>() {
            @Override
            public ArrayList<String> get(Bundle bundle, String name, ArrayList<String> def) {
                return bundle.getStringArrayList(name);
            }

            @Override
            public void set(Bundle bundle, String name, ArrayList<String> value) {
                bundle.putStringArrayList(name, value);
            }
        }, null).oneShot(), App.KEY_EDIT_DATA_FIELDS);
        editor.initForm(form);
        form.load(this, savedInstanceState);
        ac = controller.accountController(form);
        if (null == ac) {
            finish();
            controller.messageShort("Invalid arguments");
            return;
        }
        toolbar.setSubtitle(ac.name());
        progressListener = MainActivity.setupProgressListener(this, progressBar);
        new Tasks.ActivitySimpleTask<List<String>>(this){

            @Override
            protected List<String> doInBackground() {
                return ac.taskPriority();
            }

            @Override
            public void finish(List<String> result) {
                editor.setupPriorities(result);
                priorities = result;
                form.load(EditorActivity.this, savedInstanceState, App.KEY_EDIT_PRIORITY);
                editor.show(form);
                Bundle formData = form.getValue(App.KEY_EDIT_DATA);
                List<String> fields = form.getValue(App.KEY_EDIT_DATA_FIELDS);
                logger.d("Edit:", formData, fields);
                if (null != formData && null != fields) { // Have data
                    for (String f : fields) { // $COMMENT
                        form.setValue(f, formData.getString(f));
                    }
                }
            }
        }.exec();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        form.save(outState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_editor, menu);
        if (null != editor && !editor.adding(form)) { // New item mode
            menu.findItem(R.id.menu_tb_add_another).setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_tb_save:
                doSave(false);
                break;
            case R.id.menu_tb_add_another:
                doSave(true);
                break;
            case R.id.menu_tb_add_shortcut:
                createShortcut();
                break;
        }
        return true;
    }

    private void createShortcut() {
        Bundle bundle = new Bundle();
        form.save(bundle);
        bundle.remove(App.KEY_EDIT_UUID); // Just in case
        final Intent shortcutIntent = new Intent(this, EditorActivity.class);
        shortcutIntent.putExtras(bundle);
        shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        controller.input(this, "Shortcut name:", ac.name(), new DataUtil.Callback<CharSequence>() {

            @Override
            public boolean call(CharSequence value) {
                controller.createShortcut(shortcutIntent, value.toString().trim());
                return true;
            }
        }, null);
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

    @Override
    public void onBackPressed() {
        if (!form.changed()) { // No changes - just close
            super.onBackPressed();
            return;
        }
        logger.d("Changed:", form.changes());
        controller.question(this, "There are some changes, discard?", new Runnable() {

            @Override
            public void run() {
                EditorActivity.super.onBackPressed();
            }
        }, null);
    }
    
    private String propertyChange(String key, String modifier) {
        String value = form.getValue(key);
        if (TextUtils.isEmpty(value)) {
            value = "";
        }
        return String.format("%s:%s", modifier, value);
    }

    private String save() {
        if (!form.changed()) { // No change - no save
            return "Nothing has been changed";
        }
        String description = form.getValue(App.KEY_EDIT_DESCRIPTION);
        if (TextUtils.isEmpty(description)) { // Empty desc
            return "Description is mandatory";
        }
        List<String> changes = new ArrayList<>();
        for (String key : form.changes()) { // Make changes
            if (App.KEY_EDIT_DESCRIPTION.equals(key)) { // Direct
                changes.add(AccountController.escape(description));
            }
            if (App.KEY_EDIT_PROJECT.equals(key)) { // Direct
                changes.add(propertyChange(key, "project"));
            }
            if (App.KEY_EDIT_DUE.equals(key)) { // Direct
                changes.add(propertyChange(key, "due"));
            }
            if (App.KEY_EDIT_SCHEDULED.equals(key)) { // Direct
                changes.add(propertyChange(key, "scheduled"));
            }
            if (App.KEY_EDIT_WAIT.equals(key)) { // Direct
                changes.add(propertyChange(key, "wait"));
            }
            if (App.KEY_EDIT_UNTIL.equals(key)) { // Direct
                changes.add(propertyChange(key, "until"));
            }
            if (App.KEY_EDIT_RECUR.equals(key)) { // Direct
                changes.add(propertyChange(key, "recur"));
            }
            if (App.KEY_EDIT_PRIORITY.equals(key)) { // Direct
                changes.add(String.format("priority:%s", priorities
                    .get(form.getValue(App.KEY_EDIT_PRIORITY, Integer.class))));
            }
            if (App.KEY_EDIT_TAGS.equals(key)) { // Direct
                List<String> tags = new ArrayList<>();
                String tagsStr = form.getValue(App.KEY_EDIT_TAGS);
                Collections.addAll(tags, tagsStr.split(" "));
                changes.add(String.format("tags:%s", MainListAdapter.join(",", tags)));
            }
        }
        UUID uuid = form.getValue(App.KEY_EDIT_UUID);
        boolean completed = form.getValue(App.KEY_EDIT_STATUS, Integer.class) > 0;
        logger.d("Saving change:", uuid, changes, completed);
        if (uuid == null) { // Add new
            return completed? ac.taskLog(changes): ac.taskAdd(changes);
        } else {
            return ac.taskModify(uuid, changes);
        }
    }

    private void doSave(final boolean addAnother) {
        new Tasks.ActivitySimpleTask<String>(this) {

            @Override
            protected String doInBackground() {
                return save();
            }

            @Override
            public void finish(String result) {
                if (!TextUtils.isEmpty(result)) { // Failed
                    controller.messageLong(result);
                } else {
                    controller.messageShort("Task added");
                    EditorActivity.this.setResult(Activity.RESULT_OK);
                    if (addAnother) { // Keep everything except description
                        form.setValue(App.KEY_EDIT_DESCRIPTION, "");
                        form.getView(App.KEY_EDIT_DESCRIPTION).requestFocus();
                    } else { // Finish activity
                        EditorActivity.this.finish();
                    }
                }
            }
        }.exec();
    }

}
