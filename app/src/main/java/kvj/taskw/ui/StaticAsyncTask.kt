package kvj.taskw.ui

import java.lang.ref.WeakReference

import android.os.AsyncTask

abstract class StaticAsyncTask<Context, Params, Progress, Result>(activityContext: Context)
: AsyncTask<Params, Progress, Result>() {
    private val contextReference = WeakReference<Context>(activityContext)

    protected val context: Context?
        get() = contextReference.get()

    protected abstract fun Context.background(vararg params: Params): Result
    protected abstract fun Context.finish(result: Result)

    override fun doInBackground(vararg params: Params): Result? {
        val context = context ?: return null
        return context.background(*params)
    }

    override fun onPostExecute(result: Result) {
        val context = context ?: return
        context.finish(result)
    }
}
