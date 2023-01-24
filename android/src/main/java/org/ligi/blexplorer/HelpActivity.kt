package org.ligi.blexplorer

import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.annotation.MainThread
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle.Event.ON_DESTROY
import androidx.lifecycle.ViewModel
import com.uber.autodispose.AutoDispose.autoDisposable
import com.uber.autodispose.android.lifecycle.AndroidLifecycleScopeProvider.from
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import org.ligi.blexplorer.databinding.ActivityWithTextviewBinding
import org.ligi.compat.HtmlCompat
import timber.log.Timber

class HelpActivity : AppCompatActivity() {
    private val binding by lazy { ActivityWithTextviewBinding.inflate(layoutInflater) }
    private val viewModel: HelpViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        setContentView(binding.root)
        binding.contentText.movementMethod = LinkMovementMethod.getInstance()

        viewModel.loadedHtml?.let { binding.contentText.text = it }
        viewModel.loadedHtml ?: loadHelpText()
    }

    @MainThread private fun loadHelpText() {
        Single.fromCallable {
            val launcherDrawable = ContextCompat.getDrawable(this, R.drawable.ic_launcher)
                ?.apply { setBounds(0, 0, intrinsicWidth, intrinsicHeight) }
            val inputStream = assets.open("help.html")
            HtmlCompat.fromHtml(
                inputStream.bufferedReader().readText(),
                { launcherDrawable },
                null
            )
        }.subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .doOnSuccess {
            viewModel.loadedHtml = it
            binding.contentText.text = it
        }
        .doOnError { Timber.e(it, "Failed to load help text") }
        .`as`(autoDisposable(from(this, ON_DESTROY)))
        .subscribe({},{})
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        finish()
        return super.onOptionsItemSelected(item)
    }
}

class HelpViewModel : ViewModel() {
    var loadedHtml: CharSequence? = null
}