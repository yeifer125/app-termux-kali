package com.kalilinux.kali

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.termux.view.TerminalView
import com.termux.view.TerminalViewClient
import com.termux.terminal.TerminalSession
import com.termux.terminal.TerminalSessionClient
import com.jcraft.jsch.ChannelShell
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import com.jcraft.jsch.UserInfo
import java.io.InputStream
import java.io.OutputStream
import java.util.Properties
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var loginScreen: View
    private lateinit var terminalView: TerminalView
    private lateinit var passwordInput: EditText
    private lateinit var connectButton: Button
    private lateinit var rememberCheckbox: CheckBox
    private lateinit var asciiArtText: TextView
    private lateinit var statusText: TextView
    
    private var sshSession: Session? = null
    private var terminalSession: TerminalSession? = null
    private var sshOut: OutputStream? = null
    private var shellChannel: ChannelShell? = null
    
    private val outputExecutor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    private val PREFS_NAME = "KaliPrefs"
    private val KEY_PASSWORD = "saved_password"
    private val KEY_REMEMBER = "remember_me"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inicialización de vistas
        loginScreen = findViewById(R.id.loginScreen)
        terminalView = findViewById(R.id.terminalView)
        passwordInput = findViewById(R.id.passwordInput)
        connectButton = findViewById(R.id.connectButton)
        rememberCheckbox = findViewById(R.id.rememberCheckbox)
        asciiArtText = findViewById(R.id.asciiArtText)
        statusText = findViewById(R.id.statusText)

        setupLoginScreenContent()
        setupTerminalClient()
        loadSavedPassword()

        connectButton.setOnClickListener {
            val pass = passwordInput.text.toString()
            if (pass.isNotEmpty()) {
                connectButton.isEnabled = false
                passwordInput.isEnabled = false
                savePassword(pass, rememberCheckbox.isChecked)
                Thread { connectSSH(pass) }.start()
            } else {
                Toast.makeText(this, "CREDENTIALS REQUIRED", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadSavedPassword() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedPass = prefs.getString(KEY_PASSWORD, "")
        val shouldRemember = prefs.getBoolean(KEY_REMEMBER, false)
        if (shouldRemember && !savedPass.isNullOrEmpty()) {
            passwordInput.setText(savedPass)
            rememberCheckbox.isChecked = true
        }
    }

    private fun savePassword(password: String, remember: Boolean) {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
        prefs.putBoolean(KEY_REMEMBER, remember)
        if (remember) prefs.putString(KEY_PASSWORD, password) else prefs.remove(KEY_PASSWORD)
        prefs.apply()
    }

    private fun setupLoginScreenContent() {
        // ARREGLO MANUAL: Alineación borde izquierdo absoluto
        val art = "██████╗ █████╗ ██████╗ ██████╗  ██████╗ ███╗   ██╗ █████╗ ████████╗ ██████╗ \n" +
                  "██╔════╝██╔══██╗██╔══██╗██╔══██╗██╔═══██╗████╗  ██║██╔══██╗╚══██╔══╝██╔═══██╗\n" +
                  "██║     ███████║██████╔╝██████╔╝██║   ██║██╔██╗ ██║███████║   ██║   ██║   ██║\n" +
                  "██║     ██╔══██║██╔══██╗██╔══██╗██║   ██║██║╚██╗██║██╔══██║   ██║   ██║   ██║\n" +
                  "╚██████╗██║  ██║██║  ██║██████╔╝╚██████╔╝██║ ╚████║██║  ██║   ██║   ╚██████╔╝\n" +
                  " ╚═════╝╚═╝  ╚═╝╚═╝  ╚═╝╚═════╝  ╚═════╝ ╚═╝  ╚═══╝╚═╝  ╚═╝   ╚═╝    ╚═════╝ "
        
        asciiArtText.text = art
        updateStatusText(false)
    }

    private fun updateStatusText(connected: Boolean) {
        val status = if (connected) "CONNECTED" else "DISCONNECTED"
        statusText.text = "> USER: cArbonAto\n" +
                         "> MODE: ACTIVIST_HACKER\n" +
                         "> STATUS: $status\n" +
                         "> ANONIMOUS\n" +
                         "> ACCESSING LUNAR_CORE..."
    }

    private fun setupTerminalClient() {
        terminalView.setTerminalViewClient(object : TerminalViewClient {
            override fun onScale(scale: Float): Float = scale
            override fun onSingleTapUp(e: MotionEvent?) {
                terminalView.requestFocus()
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(terminalView, InputMethodManager.SHOW_IMPLICIT)
            }
            override fun onLongPress(e: MotionEvent?): Boolean = false
            override fun onKeyDown(keyCode: Int, e: KeyEvent?, session: TerminalSession?): Boolean {
                if (sshOut == null) return false
                val data = when (keyCode) {
                    KeyEvent.KEYCODE_ENTER -> "\r"
                    KeyEvent.KEYCODE_DEL -> "\u007f" 
                    KeyEvent.KEYCODE_TAB -> "\t"
                    KeyEvent.KEYCODE_ESCAPE -> "\u001b"
                    KeyEvent.KEYCODE_DPAD_UP -> "\u001b[A"
                    KeyEvent.KEYCODE_DPAD_DOWN -> "\u001b[B"
                    KeyEvent.KEYCODE_DPAD_RIGHT -> "\u001b[C"
                    KeyEvent.KEYCODE_DPAD_LEFT -> "\u001b[D"
                    else -> null
                }
                if (data != null) { sendToSsh(data); return true }
                return false 
            }
            override fun onKeyUp(keyCode: Int, e: KeyEvent?): Boolean = false
            override fun onCodePoint(codePoint: Int, ctrlDown: Boolean, session: TerminalSession?): Boolean {
                if (sshOut == null) return false
                if (ctrlDown && codePoint >= 97 && codePoint <= 122) {
                    sendToSsh((codePoint - 96).toChar().toString())
                    return true
                }
                val chars = Character.toChars(codePoint)
                sendToSsh(String(chars))
                return true
            }
            override fun onEmulatorSet() {}
            override fun copyModeChanged(copyMode: Boolean) {}
            override fun isTerminalViewSelected(): Boolean = true
            override fun readControlKey(): Boolean = false
            override fun readAltKey(): Boolean = false
            override fun readShiftKey(): Boolean = false
            override fun readFnKey(): Boolean = false
            override fun shouldBackButtonBeMappedToEscape(): Boolean = false
            override fun shouldEnforceCharBasedInput(): Boolean = true
            override fun shouldUseCtrlSpaceWorkaround(): Boolean = false
            override fun logError(tag: String?, message: String?) { Log.e(tag, message ?: "") }
            override fun logWarn(tag: String?, message: String?) {}
            override fun logInfo(tag: String?, message: String?) {}
            override fun logDebug(tag: String?, message: String?) {}
            override fun logVerbose(tag: String?, message: String?) {}
            override fun logStackTraceWithMessage(tag: String?, message: String?, e: Exception?) {}
            override fun logStackTrace(tag: String?, e: Exception?) {}
        })
    }

    private fun sendToSsh(data: String) {
        outputExecutor.execute {
            try {
                sshOut?.write(data.toByteArray(Charsets.UTF_8))
                sshOut?.flush()
            } catch (e: Exception) { Log.e("SSH_OUT", "Error enviando: ${e.message}") }
        }
    }

    private fun connectSSH(password: String) {
        try {
            val jsch = JSch()
            jsch.removeAllIdentity()
            sshSession = jsch.getSession("fsociety", "192.168.50.12", 22)
            sshSession?.setPassword(password)
            sshSession?.userInfo = object : UserInfo {
                override fun getPassphrase(): String? = null
                override fun getPassword(): String = password
                override fun promptPassword(message: String?): Boolean = true
                override fun promptPassphrase(message: String?): Boolean = false
                override fun promptYesNo(message: String?): Boolean = true
                override fun showMessage(message: String?) {}
            }
            val config = Properties()
            config["StrictHostKeyChecking"] = "no"
            sshSession?.setConfig(config)
            sshSession?.connect(15000)

            shellChannel = sshSession?.openChannel("shell") as ChannelShell
            shellChannel?.setPtyType("xterm-256color")
            val inputStream = shellChannel?.inputStream
            sshOut = shellChannel?.outputStream
            shellChannel?.connect(10000)

            mainHandler.post {
                hideKeyboard()
                updateStatusText(true)
                loginScreen.visibility = View.GONE
                terminalView.visibility = View.VISIBLE
                setupTerminal(inputStream!!)
            }
        } catch (e: Exception) {
            Log.e("SSH_KALI", "Error: ${e.message}")
            mainHandler.post {
                connectButton.isEnabled = true
                passwordInput.isEnabled = true
                Toast.makeText(this, "Fallo en la conexión", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(passwordInput.windowToken, 0)
    }

    private fun setupTerminal(sshIn: InputStream) {
        try {
            val client = object : TerminalSessionClient {
                override fun onTextChanged(session: TerminalSession) { terminalView.onScreenUpdated() }
                override fun onSessionFinished(session: TerminalSession) {
                    mainHandler.post {
                        if (shellChannel?.isConnected == false) {
                            terminalView.visibility = View.GONE
                            loginScreen.visibility = View.VISIBLE
                            connectButton.isEnabled = true
                            passwordInput.isEnabled = true
                            updateStatusText(false)
                            Toast.makeText(this@MainActivity, "Sesión terminada", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                override fun onBell(session: TerminalSession) {}
                override fun onColorsChanged(session: TerminalSession) {}
                override fun onTerminalCursorStateChange(state: Boolean) {}
                override fun onTitleChanged(session: TerminalSession) {}
                override fun onCopyTextToClipboard(session: TerminalSession?, text: String) {
                    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("Terminal", text))
                }
                override fun onPasteTextFromClipboard(session: TerminalSession?) {
                    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.primaryClip?.getItemAt(0)?.text?.let { sendToSsh(it.toString()) }
                }
                override fun getTerminalCursorStyle(): Int = 0
                override fun logError(tag: String?, message: String?) {}
                override fun logWarn(tag: String?, message: String?) {}
                override fun logInfo(tag: String?, message: String?) {}
                override fun logDebug(tag: String?, message: String?) {}
                override fun logVerbose(tag: String?, message: String?) {}
                override fun logStackTraceWithMessage(tag: String?, message: String?, e: Exception?) {}
                override fun logStackTrace(tag: String?, e: Exception?) {}
            }

            // Usamos 'cat' como proceso mudo para evitar conflictos
            terminalSession = TerminalSession("/system/bin/cat", filesDir.absolutePath, arrayOf("cat"), arrayOf(), 5000, client)
            
            terminalView.setTextSize(14) 
            terminalView.post {
                terminalView.attachSession(terminalSession)
                terminalView.requestFocus()
                
                // LIMPIEZA INICIAL: Esperamos un instante y reseteamos el emulador
                mainHandler.postDelayed({
                    terminalSession?.getEmulator()?.reset()
                    terminalView.onScreenUpdated()
                }, 200)

                val columns = terminalView.mEmulator?.mColumns ?: 80
                val rows = terminalView.mEmulator?.mRows ?: 24
                outputExecutor.execute {
                    try { shellChannel?.setPtySize(columns, rows, 0, 0) } catch (e: Exception) {}
                }
            }

            Thread {
                val buffer = ByteArray(16384)
                var bytesRead = 0
                try {
                    while (shellChannel?.isConnected == true) {
                        bytesRead = sshIn.read(buffer)
                        if (bytesRead == -1) break
                        val finalBytesRead = bytesRead
                        val dataCopy = buffer.copyOf(finalBytesRead)
                        mainHandler.post {
                            terminalSession?.getEmulator()?.append(dataCopy, finalBytesRead)
                            terminalView.onScreenUpdated()
                        }
                    }
                } catch (e: Exception) { Log.e("SSH_READ", "Cerrado") }
                finally {
                    mainHandler.post {
                        shellChannel?.disconnect()
                        client.onSessionFinished(terminalSession!!)
                    }
                }
            }.start()

        } catch (e: Exception) { Log.e("TERMINAL_KALI", "Error: ${e.message}") }
    }

    override fun onDestroy() {
        super.onDestroy()
        shellChannel?.disconnect()
        sshSession?.disconnect()
        outputExecutor.shutdown()
    }
}
