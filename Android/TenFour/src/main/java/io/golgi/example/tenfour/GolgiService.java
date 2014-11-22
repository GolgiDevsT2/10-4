package io.golgi.example.tenfour;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import com.openmindnetworks.golgi.api.GolgiAPI;
import com.openmindnetworks.golgi.api.GolgiAPIHandler;

import io.golgi.apiimpl.android.GolgiAbstractService;
import io.golgi.example.tenfour.gen.GolgiKeys;
import io.golgi.example.tenfour.gen.TenFourService.*;
import io.golgi.example.tenfour.gen.VoxPacket;

/**
 * Created by briankelly on 10/04/2014.
 */
public class GolgiService extends GolgiAbstractService {
    private static GolgiService theInstance;
    private PlaybackEngine playbackEngine;

    private static void DBG(String str){
        DBG.write("SVC", str);
    }

    public static boolean isRunning(Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (GolgiService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void readyForRegister(){

        if(theInstance == null) {
            theInstance = this;
            playbackEngine = new PlaybackEngine(this);
        }

        broadcastPacket.registerReceiver(inboundBroadcastPacket);

        registerGolgi(
                new GolgiAPIHandler() {
                    @Override
                    public void registerSuccess(){
                        DBG("Golgi registration Success");
                        TenFourActivity.golgiServiceStarted(true);
                    }

                    @Override
                    public void registerFailure() {
                        DBG("Golgi registration Failure");
                        TenFourActivity.golgiServiceStarted(false);
                    }
                },
                GolgiKeys.DEV_KEY,
                GolgiKeys.APP_KEY,
                getGolgiId(this));
    }

    public static boolean isPlayingAudio(){
        return (theInstance != null && theInstance.playbackEngine.isRunning()) ? true : false;
    }

    public static void playAudio(short[] audioData){
        theInstance.playbackEngine.play(new Transmission(audioData));
    }

    private broadcastPacket.RequestReceiver inboundBroadcastPacket = new broadcastPacket.RequestReceiver(){
        @Override
        public void receiveFrom(broadcastPacket.ResultSender resultSender, VoxPacket pkt) {
            byte[] rawAudio = pkt.getVoxData();
            short[] audioData = new short[rawAudio.length/2];
            short sval;
            resultSender.success();
            DBG.write("Inbound Voice Packet");
            int i, j;
            for(i = j = 0; i < rawAudio.length; i += 2){
                sval = (short)(((short)rawAudio[i]) & 0xff);
                sval += (short)(((short)rawAudio[i+1]) & 0xff) * 256;
                audioData[j++] = sval;
            }
            if(pkt.getDevId().compareTo(getGolgiId(GolgiService.this)) != 0) {
                GolgiService.playAudio(audioData);
            }
            else{
                DBG.write("Not playing our own transmission");
            }
        }
    };

    private static SharedPreferences getSharedPrefs(Context context){
        return context.getSharedPreferences("TenFour", Context.MODE_PRIVATE);
    }

    public static String getGolgiId(Context context){
        SharedPreferences sharedPrefs = getSharedPrefs(context);
        String golgiId =  sharedPrefs.getString("GOLGI-ID", "");

        if(golgiId.length() == 0){
            StringBuffer sb = new StringBuffer();
            for(int i = 0; i < 20; i++){
                sb.append((char)('A' + (int)(Math.random() * ('z' - 'A'))));
            }

            golgiId = sb.toString();
            sharedPrefs.edit().putString("GOLGI-ID", golgiId).commit();
        }
        DBG("GolgiID: '" + golgiId + "'");

        return golgiId;
    }

    public static int getChannel(Context context){
        SharedPreferences sharedPrefs = getSharedPrefs(context);
        return sharedPrefs.getInt("CHANNEL", 11);
    }

    public static void setChannel(Context context, int value){
        SharedPreferences sharedPrefs = getSharedPrefs(context);
        SharedPreferences.Editor e = sharedPrefs.edit();
        e.putInt("CHANNEL", value);
        e.commit();
    }

    public static boolean getOnlineState(Context context){
        SharedPreferences sharedPrefs = getSharedPrefs(context);
        return sharedPrefs.getBoolean("ONLINE", false);
    }

    public static void setOnlineState(Context context, boolean online){
        SharedPreferences sharedPrefs = getSharedPrefs(context);
        SharedPreferences.Editor e = sharedPrefs.edit();
        e.putBoolean("ONLINE", online);
        e.commit();
    }



}
