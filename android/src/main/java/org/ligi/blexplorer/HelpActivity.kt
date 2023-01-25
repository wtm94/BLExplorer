package org.ligi.blexplorer

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import org.ligi.blexplorer.databinding.ActivityWithTextviewBinding
import org.ligi.compat.HtmlCompat
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint class HelpActivity : AppCompatActivity() {
    private val binding by lazy { ActivityWithTextviewBinding.inflate(layoutInflater) }
    private val viewModel: HelpViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        setContentView(binding.root)

        binding.contentText.movementMethod = LinkMovementMethod.getInstance()
        viewModel.helpText.observe(this) { binding.contentText.text = it }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        finish()
        return super.onOptionsItemSelected(item)
    }
}

@HiltViewModel class HelpViewModel @Inject constructor(@ApplicationContext context: Context) : ViewModel() {
    internal val helpText: LiveData<CharSequence> = HelpTextLiveData(context)
}

@SuppressLint("CheckResult")
private class HelpTextLiveData(context: Context) : LiveData<CharSequence>() {
    init {
        Single.fromCallable {
            val launcherDrawable = ContextCompat.getDrawable(context, R.drawable.ic_launcher)
                ?.apply { setBounds(0, 0, intrinsicWidth, intrinsicHeight) }
            val inputStream = context.assets.open("help.html")
            HtmlCompat.fromHtml(
                inputStream.bufferedReader().readText(),
                { launcherDrawable },
                null
            )
        }.subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .doOnSuccess { value = it }
        .doOnError { Timber.e(it, "Failed to load help text") }
        .subscribe({},{})
    }
}