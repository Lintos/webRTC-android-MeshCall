package com.remotemonster.meshcall;
import android.databinding.DataBindingUtil;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;

import com.remotemonster.meshcall.databinding.ActivityMeshcallBinding;
import com.remotemonster.sdk.RemonCall;

import org.webrtc.SurfaceViewRenderer;

public class MeshCallActivity extends AppCompatActivity {
    private static final String TAG = "MESH_LOG";
    /* Remon Key*/
    private static final String REMON_SERVICE_ID = "hyungeun.jo@smoothy.co";
    private static final String REMON_KEY = "fd4d4ff5952ede14a8ecc453ad2f629bb33ff1e9380674f5";

    /* Remon SDK*/
    private RemonCall remonCall_Local;
    private RemonCall remonCall_0;
    private RemonCall remonCall_1;
    private RemonCall remonCall_2;
    private RemonCall remonCall_3;
    private ActivityMeshcallBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_meshcall);
        setWidgetToArray();
        setEventListener();
        createRemonLocal();
    }

    private RemonCall remonCalls[];
    private SurfaceViewRenderer surfViews[];
    private EditText etCalls[];
    private Button btnConnects[];
    private Button btnCloses[];
    private void setWidgetToArray() {
        remonCalls = new RemonCall[]{remonCall_0, remonCall_1, remonCall_2, remonCall_3};
        surfViews = new SurfaceViewRenderer[]{binding.surfRendererRemote0, binding.surfRendererRemote1, binding.surfRendererRemote2, binding.surfRendererRemote3};
        etCalls = new EditText[]{binding.etCall0, binding.etCall1, binding.etCall2, binding.etCall3};
        btnConnects = new Button[]{binding.btnConnect0, binding.btnConnect1, binding.btnConnect2, binding.btnConnect3};
        btnCloses = new Button[]{binding.btnClose0, binding.btnClose1, binding.btnClose2, binding.btnClose3};
    }

    private void setEventListener() {
        for (int i = 0; i < btnConnects.length; i++) {
            int finalI = i;
            btnConnects[finalI].setOnClickListener(view -> runOnUiThread(() -> {
                surfViews[finalI].setEnableHardwareScaler(true);
                remonCalls[finalI] = RemonCall.builder()
                        .context(MeshCallActivity.this)
                        .remoteView(surfViews[finalI])
                        .serviceId(REMON_SERVICE_ID)
                        .key(REMON_KEY)
                        .build();

                remonCalls[finalI].connect(etCalls[finalI].getText().toString());
                remonCalls[finalI].onComplete(() -> runOnUiThread(() -> Log.i(TAG, "remonCall_" + finalI + " onComplete()")));
                remonCalls[finalI].onClose(closeType -> runOnUiThread(() -> {
                    Log.i(TAG, "remonCall_" + finalI + " onClose()");
                    surfViews[finalI].release();
                    surfViews[finalI].clearImage();
                    surfViews[finalI].invalidate();
                }));
            }));

            btnCloses[finalI].setOnClickListener(view -> runOnUiThread(() -> remonCalls[finalI].close()));
        }
    }

    private void createRemonLocal() {
        remonCall_Local = RemonCall.builder()
                .context(MeshCallActivity.this)
                .localView(binding.surfRendererLocal)
                .serviceId(REMON_SERVICE_ID)
                .key(REMON_KEY)
                .build();
        remonCall_Local.onInit(() -> runOnUiThread(() -> remonCall_Local.showLocalVideo()));
    }
}
