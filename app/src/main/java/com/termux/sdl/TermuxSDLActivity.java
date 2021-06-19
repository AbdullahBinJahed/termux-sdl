package com.termux.sdl;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import org.libsdl.app.SDLActivity;

public class TermuxSDLActivity extends SDLActivity {

    private static final String TAG = "TermuxSDLActivity";

    // the default main program 
    private String sdlmain = "libmain.so";

    // the SDL2 libraries version
    private enum SDLVersion {
        SDL2, SDL2_image, SDL2_mixer, SDL2_net, SDL2_ttf, SDL2_gfx;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if((getIntent().getFlags() & Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT) != 0) {
            this.finish();
            return;
        }
        super.onCreate(savedInstanceState);

        String permission =  Manifest.permission.WRITE_EXTERNAL_STORAGE;
        if(!hasPermission(permission))
            applyPermission(permission);

        sdlmain = getIntent().getStringExtra("sdlmain");
        Log.i(TAG, "sdlmain: " + sdlmain);
        copyLibFile();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        // TODO: Implement this method
        super.onNewIntent(intent);
        // recreate();
    }

    @Override
    protected String[] getArguments() {
        String argv = getIntent().getStringExtra("argv");
        if(argv != null && !argv.isEmpty())
            return argv.trim().split(" ");
        else 
            return super.getArguments();
    }

    @Override
    protected String getMainSharedObject() {
        if(sdlmain != null && !sdlmain.isEmpty()) 
            return sdlmain;
        else 
            return super.getMainSharedObject();
    }

    public void copyLibFile() {
        if(sdlmain != null && !sdlmain.isEmpty()) {
            Path sdl = Paths.get(sdlmain);
            if(Files.exists(sdl)) {
                Path dir = Paths.get(getCacheDir().getParentFile().getAbsolutePath() + "/tmpdir");
                Path file = dir.resolve(sdl.getFileName());

                try {
                    if(!Files.exists(dir)) 
                        Files.createDirectories(dir);

                    // copy libxxx.so to internal directory
                    Files.copy(sdl, file,  StandardCopyOption.REPLACE_EXISTING);
                    Runtime.getRuntime().exec("chmod 755 " + file.toAbsolutePath()).waitFor();

                    // Environment variables must be set, otherwise the program will not run correctly
                    String pwd = sdl.getParent().toAbsolutePath().toString();
                    Log.i(TAG, "chdir to: " + pwd);
                    JNI.chDir(pwd);
                    JNI.setEnv("PWD", pwd, true);
                    // sdlmain = /data/user/0/com.termux.sdl/tmpdir/libxxx.so
                    sdlmain = file.toAbsolutePath().toString();
                } catch(Exception e) {
                    showErrorDialog(e.getMessage());
                    e.printStackTrace();
                } 
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        deleteLibFile();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        deleteLibFile();
    }

    public void deleteLibFile() {
        try {
            // delete /data/user/0/com.termux.sdl/tmpdir/libxxx.so
            if(sdlmain != null && !sdlmain.isEmpty())
                Files.deleteIfExists(Paths.get(sdlmain));
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    // check permission
    public boolean hasPermission(String permission) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            return checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
        else
            return true;
    }

    public void applyPermission(String permission) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if(shouldShowRequestPermissionRationale(permission)) {
                Toast.makeText(this, "request read sdcard permmission", Toast.LENGTH_SHORT).show();
            }
            requestPermissions(new String[] {permission}, 0);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_action, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
        case R.id.action_settings:
            break;
        case R.id.action_ffplay:
            showFFplayDialog();
            break;
        case R.id.action_about:
            showAboutDialog();
            break;
        case R.id.action_logcat:
            showLogcatDialog();
            break;
        }
        return super.onOptionsItemSelected(item);
    }

    // show ffplay dialog, you can input the ffplay command
    public void showFFplayDialog() {
        final View view = getLayoutInflater().inflate(R.layout.ffplay_cmd_dialog, null);
        final EditText cmdEditText = view.findViewById(R.id.cmd_text);
        final String cmd = mPreferences.getString("cmd", "").trim();

        if(!cmd.isEmpty())
            cmdEditText.setText(cmd);
        else 
            cmdEditText.setHint("Usage: ffplay [options] input_file");

        cmdEditText.requestFocus();

        // create dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("ffplay");
        builder.setView(view);
        builder.setPositiveButton(android.R.string.ok, (dialog, which) -> {
            dialog.dismiss();
            SDLActivity.nativeSendQuit();
            //SDLActivity.nativeQuit();
            String text = cmdEditText.getText().toString().trim();
            mPreferences.edit().putString("cmd", text).commit();

            if(text.startsWith("ffplay")) 
                text = text.substring(6, text.length()).trim();

            Intent intent = new Intent(TermuxSDLActivity.this, TermuxFFplayActivity.class);
            intent.putExtra("argv", text);
            startActivity(intent);
            finish();
        });
        builder.setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.dismiss());
        builder.setOnCancelListener(dialog -> dialog.dismiss());
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    // show the version information 
    public void showAboutDialog() {
        String versionName = null;
        try {
            PackageManager pkg = getPackageManager();
            PackageInfo info = pkg.getPackageInfo(getPackageName(), 0);
            versionName = pkg.getApplicationLabel(info.applicationInfo) + ": " + info.versionName;
        } catch(PackageManager.NameNotFoundException e) {
            versionName = e.getMessage();
        }

        View view = getLayoutInflater().inflate(R.layout.about_dialog, null);
        final TextView textView = view.findViewById(R.id.about_text);
        textView.setText(versionName + "\nhttps://github.com/Lzhiyong/termux-sdl\n\n"
                         + getString(R.string.sdl_version) + ": " + JNI.getSDLVersion(SDLVersion.SDL2.ordinal()) + "\n"
                         + "https://www.libsdl.org\n\n"
                         + getString(R.string.sdl_image_version) + ": " + JNI.getSDLVersion(SDLVersion.SDL2_image.ordinal()) + "\n"
                         + "https://www.libsdl.org/projects/SDL_image\n\n"
                         + getString(R.string.sdl_mixer_version) + ": " + JNI.getSDLVersion(SDLVersion.SDL2_mixer.ordinal()) + "\n"
                         + "https://www.libsdl.org/projects/SDL_mixer\n\n"
                         + getString(R.string.sdl_net_version) + ": " + JNI.getSDLVersion(SDLVersion.SDL2_net.ordinal()) + "\n"
                         + "https://www.libsdl.org/projects/SDL_net\n\n"
                         + getString(R.string.sdl_ttf_version) + ": " + JNI.getSDLVersion(SDLVersion.SDL2_ttf.ordinal()) + "\n"
                         + "https://www.libsdl.org/projects/SDL_ttf\n\n"
                         + getString(R.string.sdl_gfx_version) + ": " + JNI.getSDLVersion(SDLVersion.SDL2_gfx.ordinal()) + "\n"
                         + "http://www.ferzkopp.net/wordpress/2016/01/02/sdl_gfx-sdl2_gfx\n\n"
                         + getString(R.string.ffmpeg_version) + ": " + JNI.getFFmpegVersion() + "\n"
                         + "http://ffmpeg.org\n"
                         );

        new AlertDialog.Builder(this)
            .setTitle(getString(R.string.about_dialog))
            .setView(view)
            .setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.dismiss())
            .setOnCancelListener(dialog -> dialog.dismiss())
            .show();
    }
}
