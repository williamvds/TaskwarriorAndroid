package kvj.taskw.ui;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;
import org.kvj.bravo7.form.FormController;
import org.kvj.bravo7.log.Logger;

import java.util.List;

import kvj.taskw.App;
import kvj.taskw.R;
import kvj.taskw.data.Controller;
import kvj.taskw.data.ReportInfo;

/**
 * Created by vorobyev on 11/19/15.
 */
public class MainList extends Fragment {

    private RecyclerView list = null;
    private ReportInfo info = null;
    Controller controller = App.controller();
    Logger logger = Logger.forInstance(this);
    private MainListAdapter adapter = null;
    private String account = null;

    @Nullable
    @Override
    public View onCreateView(@NotNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_list, container, false);
        list = view.findViewById(R.id.list_main_list);
        list.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new MainListAdapter();
        list.setAdapter(adapter);
        return view;
    }

    private static class LoadReportTask extends StaticAsyncTask<MainList, Void, Void, ReportInfo> {
        private String report;
        private String query;
        private Runnable callback;

        LoadReportTask(MainList frag, String report, String query, Runnable callback) {
            super(frag);
            this.report = report;
            this.query = query;
            this.callback = callback;
        }

        @Override
        protected ReportInfo background(MainList frag, Void... params) {
            frag.logger.d("Load:", query, report);
            return frag.controller.accountController(frag.account).taskReportInfo(report, query);
        }

        @Override
        protected void finish(MainList frag, ReportInfo result) {
            frag.info = result;
            if (callback != null) callback.run();
            frag.reload();
        }
    }

    public void load(final FormController form, final Runnable afterLoad) {
        this.account = form.getValue(App.KEY_ACCOUNT);
        final String report = form.getValue(App.KEY_REPORT);
        final String query = form.getValue(App.KEY_QUERY);
        new LoadReportTask(this, report, query, afterLoad).execute();
    }

    private static class LoadResultsTask extends StaticAsyncTask<MainList, Void, Void, List<JSONObject>> {
        LoadResultsTask(MainList frag) {
            super(frag);
        }

        @Override
        protected List<JSONObject> background(MainList frag, Void... params) {
            frag.logger.d("Exec:", frag.info.query);
            List<JSONObject> list = frag.controller.accountController(frag.account).taskList(frag.info.query);
            frag.info.sort(list); // Sorted according to report spec.
            return list;
        }

        @Override
        public void finish(MainList frag, List<JSONObject> result) {
            frag.adapter.update(result, frag.info);
        }
    }

    public void reload() {
        if (null == info || null == account) return;
        // Load all items
        new LoadResultsTask(this).execute();
    }

    public void listener(MainListAdapter.ItemListener listener) {
        adapter.setListener(listener);
    }


    public ReportInfo reportInfo() {
        return info;
    }
}
