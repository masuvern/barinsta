package awais.instagrabber.activities

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.DocumentsContract
import android.util.Log
import android.view.View
import androidx.lifecycle.ViewModelProvider
import awais.instagrabber.R
import awais.instagrabber.databinding.ActivityDirectorySelectBinding
import awais.instagrabber.dialogs.ConfirmDialogFragment
import awais.instagrabber.utils.AppExecutors.mainThread
import awais.instagrabber.utils.Constants
import awais.instagrabber.utils.extensions.TAG
import awais.instagrabber.viewmodels.DirectorySelectActivityViewModel
import java.io.IOException
import java.io.PrintWriter
import java.io.StringWriter

class DirectorySelectActivity : BaseLanguageActivity() {
    private var initialUri: Uri? = null

    private lateinit var binding: ActivityDirectorySelectBinding
    private lateinit var viewModel: DirectorySelectActivityViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDirectorySelectBinding.inflate(layoutInflater)
        setContentView(binding.root)
        viewModel = ViewModelProvider(this).get(DirectorySelectActivityViewModel::class.java)
        val intent = intent
        viewModel.setInitialUri(intent)
        setupObservers()
        binding.selectDir.setOnClickListener { openDirectoryChooser() }
        initialUri = intent.getParcelableExtra(Constants.EXTRA_INITIAL_URI)
    }

    private fun setupObservers() {
        viewModel.message.observe(this, { message: String? -> binding.message.text = message })
        viewModel.prevUri.observe(this, { prevUri: String? ->
            if (prevUri == null) {
                binding.prevUri.visibility = View.GONE
                binding.message2.visibility = View.GONE
                return@observe
            }
            binding.prevUri.text = prevUri
            binding.prevUri.visibility = View.VISIBLE
            binding.message2.visibility = View.VISIBLE
        })
        viewModel.dirSuccess.observe(this, { success: Boolean -> binding.selectDir.visibility = if (success) View.GONE else View.VISIBLE })
        viewModel.isLoading.observe(this, { loading: Boolean ->
            binding.message.visibility = if (loading) View.GONE else View.VISIBLE
            binding.loadingIndicator.visibility = if (loading) View.VISIBLE else View.GONE
        })
    }

    private fun openDirectoryChooser() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && initialUri != null) {
            intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, initialUri)
        }
        try {
            startActivityForResult(intent, SELECT_DIR_REQUEST_CODE)
        } catch (e: ActivityNotFoundException) {
            Log.e(TAG, "openDirectoryChooser: ", e)
            showErrorDialog(getString(R.string.no_directory_picker_activity))
        } catch (e: Exception) {
            Log.e(TAG, "openDirectoryChooser: ", e)
        }
    }

    @SuppressLint("StringFormatInvalid")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != SELECT_DIR_REQUEST_CODE) return
        if (resultCode != RESULT_OK) {
            showErrorDialog(getString(R.string.select_a_folder))
            return
        }
        if (data == null || data.data == null) {
            showErrorDialog(getString(R.string.select_a_folder))
            return
        }
        val authority = data.data?.authority
        if ("com.android.externalstorage.documents" != authority) {
            showErrorDialog(getString(R.string.dir_select_no_download_folder, authority))
            return
        }
        mainThread.execute({
            try {
                viewModel.setupSelectedDir(data)
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish()
            } catch (e: Exception) {
                // Should not come to this point.
                // If it does, we have to show this error to the user so that they can report it.
                try {
                    StringWriter().use { sw ->
                        PrintWriter(sw).use { pw ->
                            e.printStackTrace(pw)
                            showErrorDialog("Please report this error to the developers:\n\n$sw")
                        }
                    }
                } catch (ioException: IOException) {
                    Log.e(TAG, "onActivityResult: ", ioException)
                }
            }
        }, 500)
    }

    private fun showErrorDialog(message: String) {
        val dialogFragment = ConfirmDialogFragment.newInstance(
            ERROR_REQUEST_CODE,
            R.string.error,
            message,
            R.string.ok,
            0,
            0
        )
        dialogFragment.show(supportFragmentManager, ConfirmDialogFragment::class.java.simpleName)
    }

    companion object {
        const val SELECT_DIR_REQUEST_CODE = 0x01
        private const val ERROR_REQUEST_CODE = 0x02
    }
}