package kvj.taskw.data

import java.util.UUID

import android.os.Bundle

import org.kvj.bravo7.form.BundleAdapter

class UUIDBundleAdapter : BundleAdapter<UUID>() {
    override fun set(bundle: Bundle?, name: String?, value: UUID?) {
        bundle?.putSerializable(name, value)
    }

    override fun get(bundle: Bundle?, name: String?, def: UUID?): UUID?
        = bundle?.getSerializable(name) as UUID?
}
