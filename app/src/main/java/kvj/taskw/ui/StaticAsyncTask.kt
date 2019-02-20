package kvj.taskw.ui

import java.lang.ref.WeakReference

import android.os.AsyncTask

abstract class StaticAsyncTask<Context, Params, Progress, Result>(activityContext: Context)
: AsyncTask<Params, Progress, Result>() {
    private val contextReference = WeakReference<Context>(activityContext)

    protected val context: Context?
        get() = contextReference.get()

    protected abstract fun background(context: Context, vararg params: Params): Result
    protected abstract fun finish(context: Context, result: Result)

    override fun doInBackground(vararg params: Params): Result? {
        val context = context ?: return null
        return background(context, *params)
    }

    override fun onPostExecute(result: Result) {
        val context = context ?: return
        finish(context, result)
    }
}
