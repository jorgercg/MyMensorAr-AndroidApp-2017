package com.mymensorar;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;
import android.util.Xml;

import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.services.s3.model.ObjectMetadata;


import org.apache.commons.io.FileUtils;
import org.xmlpull.v1.XmlSerializer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;

public class ConfigFileCreator {

    private static final String TAG = "ConfigFileCreator";

    public static void createVpsfile(Context context,
                                     File directory,
                                     String fileName,
                                     TransferUtility transferUtility,
                                     String vpsRemotePath,
                                     String mymensorAccount){
        short assetId = 1;
        String frequencyUnit = Constants.frequencyUnit;
        int frequencyValue = Constants.frequencyValue;
        short qtyVps =Constants.maxQtyVps;
        float tolerancePosition = Constants.tolerancePosition;
        float toleranceRotation = Constants.toleranceRotation;
        boolean vpArIsConfigured[] = new boolean[qtyVps];
        boolean vpIsVideo[] = new boolean[qtyVps];
        int vpXCameraDistance[] = new int[qtyVps];
        int vpYCameraDistance[] = new int[qtyVps];
        int vpZCameraDistance[] = new int[qtyVps];
        int vpXCameraRotation[] = new int[qtyVps];
        int vpYCameraRotation[] = new int[qtyVps];
        int vpZCameraRotation[] = new int[qtyVps];
        String vpLocationDesText[] = new String[qtyVps];
        short vpMarkerlessMarkerWidth[] = new short[qtyVps];
        short vpMarkerlessMarkerHeigth[] = new short[qtyVps];
        boolean vpIsAmbiguous[] = new boolean[qtyVps];
        boolean vpFlashTorchIsOn[] = new boolean[qtyVps];
        boolean vpIsSuperSingle[] = new boolean[qtyVps];
        int vpSuperMarkerId[] = new int[qtyVps];
        for (int j=0; j<(qtyVps); j++) {
            vpArIsConfigured[j]=false;
            vpIsVideo[j]=false;
            vpXCameraDistance[j]=0;
            vpYCameraDistance[j]=0;
            vpZCameraDistance[j]=0;
            vpXCameraRotation[j]=0;
            vpYCameraRotation[j]=0;
            vpZCameraRotation[j]=0;
            vpLocationDesText[j] = context.getString(R.string.vp_capture_placeholder_description)+j;
            vpMarkerlessMarkerWidth[j] = Constants.standardMarkerlessMarkerWidth;
            vpMarkerlessMarkerHeigth[j] = Constants.standardMarkerlessMarkerHeigth;
            vpIsAmbiguous[j]=false;
            vpFlashTorchIsOn[j]=false;
            vpIsSuperSingle[j]=false;
            vpSuperMarkerId[j] = 0;
        }
        // Saving Vps Data initial configuration.
        try
        {
            XmlSerializer xmlSerializer = Xml.newSerializer();
            StringWriter writer = new StringWriter();
            xmlSerializer.setOutput(writer);
            xmlSerializer.startDocument("UTF-8", true);
            xmlSerializer.text("\n");
            xmlSerializer.startTag("","VpsData");
            xmlSerializer.text("\n");
            xmlSerializer.text("\t");
            xmlSerializer.startTag("","Parameters");
            xmlSerializer.text("\n");
            xmlSerializer.text("\t");
            xmlSerializer.text("\t");
            xmlSerializer.startTag("","AssetId");
            xmlSerializer.text(Short.toString(assetId));
            xmlSerializer.endTag("","AssetId");
            xmlSerializer.text("\n");
            xmlSerializer.text("\t");
            xmlSerializer.text("\t");
            xmlSerializer.startTag("","FrequencyUnit");
            xmlSerializer.text(frequencyUnit);
            xmlSerializer.endTag("","FrequencyUnit");
            xmlSerializer.text("\n");
            xmlSerializer.text("\t");
            xmlSerializer.text("\t");
            xmlSerializer.startTag("","FrequencyValue");
            xmlSerializer.text(Integer.toString(frequencyValue));
            xmlSerializer.endTag("","FrequencyValue");
            xmlSerializer.text("\n");
            xmlSerializer.text("\t");
            xmlSerializer.text("\t");
            xmlSerializer.startTag("","QtyVps");
            xmlSerializer.text(Short.toString(qtyVps));
            xmlSerializer.endTag("","QtyVps");
            xmlSerializer.text("\n");
            xmlSerializer.text("\t");
            xmlSerializer.text("\t");
            xmlSerializer.startTag("","TolerancePosition");
            xmlSerializer.text(Float.toString(tolerancePosition));
            xmlSerializer.endTag("","TolerancePosition");
            xmlSerializer.text("\n");
            xmlSerializer.text("\t");
            xmlSerializer.text("\t");
            xmlSerializer.startTag("","ToleranceRotation");
            xmlSerializer.text(Float.toString(toleranceRotation));
            xmlSerializer.endTag("","ToleranceRotation");
            xmlSerializer.text("\n");
            xmlSerializer.text("\t");
            xmlSerializer.endTag("","Parameters");
            for (int i=0; i<(qtyVps); i++)
            {
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","Vp");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","VpNumber");
                xmlSerializer.text(Integer.toString(i));
                xmlSerializer.endTag("","VpNumber");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","VpDescFileSize");
                xmlSerializer.text(Long.toString(Constants.VpDescFileSize));
                xmlSerializer.endTag("","VpDescFileSize");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","VpMarkerFileSize");
                xmlSerializer.text(Long.toString(Constants.VpMarkerFileSize));
                xmlSerializer.endTag("","VpMarkerFileSize");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","VpArIsConfigured");
                xmlSerializer.text(Boolean.toString(vpArIsConfigured[i]));
                xmlSerializer.endTag("","VpArIsConfigured");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","VpIsVideo");
                xmlSerializer.text(Boolean.toString(vpIsVideo[i]));
                xmlSerializer.endTag("","VpIsVideo");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","VpXCameraDistance");
                xmlSerializer.text(Integer.toString(vpXCameraDistance[i]));
                xmlSerializer.endTag("","VpXCameraDistance");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","VpYCameraDistance");
                xmlSerializer.text(Integer.toString(vpYCameraDistance[i]));
                xmlSerializer.endTag("","VpYCameraDistance");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","VpZCameraDistance");
                xmlSerializer.text(Integer.toString(vpZCameraDistance[i]));
                xmlSerializer.endTag("","VpZCameraDistance");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","VpXCameraRotation");
                xmlSerializer.text(Integer.toString(vpXCameraRotation[i]));
                xmlSerializer.endTag("","VpXCameraRotation");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","VpYCameraRotation");
                xmlSerializer.text(Integer.toString(vpYCameraRotation[i]));
                xmlSerializer.endTag("","VpYCameraRotation");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","VpZCameraRotation");
                xmlSerializer.text(Integer.toString(vpZCameraRotation[i]));
                xmlSerializer.endTag("","VpZCameraRotation");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","VpLocDescription");
                xmlSerializer.text(vpLocationDesText[i]);
                xmlSerializer.endTag("","VpLocDescription");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","VpMarkerlessMarkerWidth");
                xmlSerializer.text(Short.toString(vpMarkerlessMarkerWidth[i]));
                xmlSerializer.endTag("","VpMarkerlessMarkerWidth");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","VpMarkerlessMarkerHeigth");
                xmlSerializer.text(Short.toString(vpMarkerlessMarkerHeigth[i]));
                xmlSerializer.endTag("","VpMarkerlessMarkerHeigth");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","VpIsAmbiguous");
                xmlSerializer.text(Boolean.toString(vpIsAmbiguous[i]));
                xmlSerializer.endTag("","VpIsAmbiguous");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","VpFlashTorchIsOn");
                xmlSerializer.text(Boolean.toString(vpFlashTorchIsOn[i]));
                xmlSerializer.endTag("","VpFlashTorchIsOn");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","VpIsSuperSingle");
                xmlSerializer.text(Boolean.toString(vpIsSuperSingle[i]));
                xmlSerializer.endTag("","VpIsSuperSingle");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","VpSuperMarkerId");
                if (vpIsSuperSingle[i])
                {
                    xmlSerializer.text(Integer.toString(vpSuperMarkerId[i]));
                }
                else
                {
                    xmlSerializer.text(Integer.toString(0));
                }
                xmlSerializer.endTag("","VpSuperMarkerId");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","VpFrequencyUnit");
                xmlSerializer.text("");
                xmlSerializer.endTag("","VpFrequencyUnit");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","VpFrequencyValue");
                xmlSerializer.text(Long.toString(0));
                xmlSerializer.endTag("","VpFrequencyValue");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.endTag("","Vp");
            }
            xmlSerializer.text("\n");
            xmlSerializer.endTag("","VpsData");
            xmlSerializer.endDocument();
            String vpsConfigFileContents = writer.toString();
            try {
                File vpsConfigFile = new File(directory,fileName);
                FileUtils.writeStringToFile(vpsConfigFile,vpsConfigFileContents, UTF_8);
                ObjectMetadata myObjectMetadata = new ObjectMetadata();
                //create a map to store user metadata
                Map<String, String> userMetadata = new HashMap<String,String>();
                userMetadata.put("mymensorAccount", mymensorAccount);
                myObjectMetadata.setUserMetadata(userMetadata);
                TransferObserver observer = MymUtils.storeRemoteFile(transferUtility, (vpsRemotePath + Constants.vpsConfigFileName), Constants.BUCKET_NAME, vpsConfigFile, myObjectMetadata);
                observer.setTransferListener(new TransferListener() {
                    @Override
                    public void onStateChanged(int id, TransferState state) {
                        if (state.equals(TransferState.COMPLETED)) {
                            Log.d(TAG,"TransferListener="+state.toString());
                        }
                    }

                    @Override
                    public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
                        if (bytesTotal>0){
                            int percentage = (int) (bytesCurrent / bytesTotal * 100);
                        }

                        //Display percentage transfered to user
                    }

                    @Override
                    public void onError(int id, Exception ex) {
                        Log.e(TAG, "saveVpsData(): vpsConfigFile saving failed, see stack trace"+ ex.toString());
                    }

                });
            } catch (Exception e) {
                Log.e(TAG, "createVpsfile(): vpsFile creation failed:"+e.toString());
            }
        }
        catch (Exception e)
        {
            Log.e(TAG, "createVpsfile(): vpsFile creation failed:"+e.toString());
        }

    }


    public static void createVpsCheckedFile(Context context,
                                            File directory,
                                            String fileName,
                                            TransferUtility transferUtility,
                                            String vpsCheckedRemotePath,
                                            String mymensorAccount){

        short qtyVps=Constants.maxQtyVps;
        // Saving vpChecked state.
        try {
            XmlSerializer xmlSerializer = Xml.newSerializer();
            StringWriter writer = new StringWriter();
            xmlSerializer.setOutput(writer);
            xmlSerializer.startDocument("UTF-8", true);
            xmlSerializer.text("\n");
            xmlSerializer.startTag("", "VpsChecked");
            xmlSerializer.text("\n");
            for (int i = 0; i < qtyVps; i++) {
                xmlSerializer.text("\t");
                xmlSerializer.startTag("", "Vp");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("", "VpNumber");
                xmlSerializer.text(Integer.toString(i));
                xmlSerializer.endTag("", "VpNumber");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("", "Checked");
                xmlSerializer.text(Boolean.toString(false));
                xmlSerializer.endTag("", "Checked");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("", "PhotoTakenTimeMillis");
                xmlSerializer.text(Long.toString(0));
                xmlSerializer.endTag("", "PhotoTakenTimeMillis");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.endTag("", "Vp");
                xmlSerializer.text("\n");
            }
            xmlSerializer.endTag("", "VpsChecked");
            xmlSerializer.endDocument();
            String vpsCheckedFileContents = writer.toString();
            try {
                File vpsCheckedFile = new File(directory,fileName);
                FileUtils.writeStringToFile(vpsCheckedFile,vpsCheckedFileContents, UTF_8);
                ObjectMetadata myObjectMetadata = new ObjectMetadata();
                //create a map to store user metadata
                Map<String, String> userMetadata = new HashMap<String,String>();
                userMetadata.put("mymensorAccount", mymensorAccount);
                myObjectMetadata.setUserMetadata(userMetadata);
                TransferObserver observer = MymUtils.storeRemoteFile(transferUtility, (vpsCheckedRemotePath + Constants.vpsCheckedConfigFileName), Constants.BUCKET_NAME, vpsCheckedFile, myObjectMetadata);
                observer.setTransferListener(new TransferListener() {
                    @Override
                    public void onStateChanged(int id, TransferState state) {
                        if (state.equals(TransferState.COMPLETED)) {
                            Log.d(TAG,"SaveVpsChecked(): TransferListener="+state.toString());
                        }
                    }

                    @Override
                    public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
                        if (bytesTotal>0){
                            int percentage = (int) (bytesCurrent / bytesTotal * 100);
                        }

                        //Display percentage transfered to user
                    }

                    @Override
                    public void onError(int id, Exception ex) {
                        Log.e(TAG, "SaveVpsChecked(): vpsCheckedFile saving failed:"+ ex.toString());
                    }

                });
            } catch (Exception e)
            {
                Log.e(TAG, "createVpsCheckedFile(): file creation failed, see stack trace"+e.toString());
            }

        } catch (Exception e) {
            Log.e(TAG, "createVpsCheckedFile saving to Remote Storage failed:"+e.toString());
        }
    }


    public static void createDescvpFile(Context context,
                                        File directory,
                                        String fileName,
                                        TransferUtility transferUtility,
                                        String descvpRemotePath,
                                        int vpIndex,
                                        String mymensorAccount){

        String internalAssetsFileName = "mymensordescvp.png";
        AssetManager assetManager = context.getAssets();
        InputStream in = null;
        OutputStream out = null;
        try {
            in = assetManager.open(internalAssetsFileName);
            File outFile = new File(directory, fileName);
            out = new FileOutputStream(outFile);
            copyFile(in, out);
        } catch(IOException e) {
            Log.e(TAG, "createDescvpFile: Failed to copy asset file: " + fileName, e);
        }
        finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    // NOOP
                }
            }
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    // NOOP
                }
            }
        }
        File pictureFile = new File(context.getFilesDir(), fileName);
        ObjectMetadata myObjectMetadata = new ObjectMetadata();
        //create a map to store user metadata
        Map<String, String> userMetadata = new HashMap<String,String>();
        userMetadata.put("VP", ""+(vpIndex));
        userMetadata.put("mymensorAccount", mymensorAccount);
        //call setUserMetadata on our ObjectMetadata object, passing it our map
        myObjectMetadata.setUserMetadata(userMetadata);
        //uploading the objects
        TransferObserver observer = MymUtils.storeRemoteFile(
                transferUtility,
                descvpRemotePath+pictureFile.getName(),
                Constants.BUCKET_NAME,
                pictureFile,
                myObjectMetadata);
        observer.setTransferListener(new TransferListener() {
            @Override
            public void onStateChanged(int id, TransferState state) {
                if (state.equals(TransferState.COMPLETED)) {
                    Log.d(TAG,"createDescvpFile(): TransferListener="+state.toString());
                }
            }

            @Override
            public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
                if (bytesTotal>0){
                    int percentage = (int) (bytesCurrent / bytesTotal * 100);
                }

                //Display percentage transfered to user
            }

            @Override
            public void onError(int id, Exception ex) {
                Log.e(TAG, "createDescvpFile(): createDescvpFile saving failed:"+ ex.toString());
            }

        });
    }


    public static void createLocalDescvpFile(Context context,
                                        File directory,
                                        String fileName){

        String internalAssetsFileName = "mymensordescvp.png";
        AssetManager assetManager = context.getAssets();
        InputStream in = null;
        OutputStream out = null;
        try {
            in = assetManager.open(internalAssetsFileName);
            File outFile = new File(directory, fileName);
            out = new FileOutputStream(outFile);
            copyFile(in, out);
            Log.e(TAG, "createLocalDescvpFile: CREATED: " + fileName);
        } catch(IOException e) {
            Log.e(TAG, "createLocalDescvpFile: Failed to copy asset file: " + fileName, e);
        }
        finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    // NOOP
                }
            }
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    // NOOP
                }
            }
        }

    }


    public static void createMarkervpFile(Context context,
                                          File directory,
                                          String fileName,
                                          TransferUtility transferUtility,
                                          String markervpRemotePath,
                                          int vpIndex,
                                          String mymensorAccount){

        String internalAssetsFileName = "mymensormarkervpbw.png";
        AssetManager assetManager = context.getAssets();
        InputStream in = null;
        OutputStream out = null;
        try {
            in = assetManager.open(internalAssetsFileName);
            File outFile = new File(directory, fileName);
            out = new FileOutputStream(outFile);
            copyFile(in, out);
        } catch(IOException e) {
            Log.e(TAG, "createMarkervpFile: Failed to copy asset file: " + fileName, e);
        }
        finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    // NOOP
                }
            }
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    // NOOP
                }
            }
        }
        File markerFile = new File(context.getFilesDir(), fileName);
        ObjectMetadata myObjectMetadata = new ObjectMetadata();
        //create a map to store user metadata
        Map<String, String> userMetadata = new HashMap<String, String>();
        userMetadata.put("VP", "" + (vpIndex));
        userMetadata.put("mymensorAccount", mymensorAccount);
        //call setUserMetadata on our ObjectMetadata object, passing it our map
        myObjectMetadata.setUserMetadata(userMetadata);
        //uploading the objects
        TransferObserver observer = MymUtils.storeRemoteFile(
                transferUtility,
                markervpRemotePath + markerFile.getName(),
                Constants.BUCKET_NAME,
                markerFile,
                myObjectMetadata);
        observer.setTransferListener(new TransferListener() {
            @Override
            public void onStateChanged(int id, TransferState state) {
                if (state.equals(TransferState.COMPLETED)) {
                    Log.d(TAG,"createMarkervpFile(): TransferListener="+state.toString());
                }
            }

            @Override
            public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
                if (bytesTotal>0){
                    int percentage = (int) (bytesCurrent / bytesTotal * 100);
                }

                //Display percentage transfered to user
            }

            @Override
            public void onError(int id, Exception ex) {
                Log.e(TAG, "createMarkervpFile(): createDescvpFile saving failed:"+ ex.toString());
            }

        });
    }


    public static void createLocalMarkervpFile(Context context,
                                          File directory,
                                          String fileName){

        String internalAssetsFileName = "mymensormarkervpbw.png";
        AssetManager assetManager = context.getAssets();
        InputStream in = null;
        OutputStream out = null;
        try {
            in = assetManager.open(internalAssetsFileName);
            File outFile = new File(directory, fileName);
            out = new FileOutputStream(outFile);
            copyFile(in, out);
            Log.e(TAG, "createLocalMarkervpFile: CREATED: " + fileName);
        } catch(IOException e) {
            Log.e(TAG, "createLocalMarkervpFile: Failed to copy asset file: " + fileName, e);
        }
        finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    // NOOP
                }
            }
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    // NOOP
                }
            }
        }
    }


    private static void copyFile(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
    }

}