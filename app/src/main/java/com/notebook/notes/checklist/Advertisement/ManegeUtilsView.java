package com.notebook.notes.checklist.Advertisement;

import android.content.Context;

import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

public class ManegeUtilsView {

    public static String ssfsfsfsf;

    public static void isUtilsManege(Context context) {


//        ManegeParameter.asadpass = "ghjkltgvcxzasdew";//key
//        ManegeParameter.sdasdada = "U2FsdGVkX19yNbDXaQJ6x12iYI7d6dSHXn7bjLbYvy4=";

        ManegeParameter.asadpass = "hngldvbgtyhnmjui";
        ManegeParameter.sdasdada = "U2FsdGVkX1+J0Npsk0kzOBx4YCkts9q+xLqkCRK2vSA=";



        try {
            ssfsfsfsf = ManegeParameter.cipherSignatureSpace(ManegeParameter.asadpass, ManegeParameter.sdasdada);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (NoSuchPaddingException e) {
            throw new RuntimeException(e);
        } catch (InvalidAlgorithmParameterException e) {
            throw new RuntimeException(e);
        } catch (InvalidKeyException e) {
            throw new RuntimeException(e);
        } catch (BadPaddingException e) {
            throw new RuntimeException(e);
        } catch (IllegalBlockSizeException e) {
            throw new RuntimeException(e);
        }
    }
}
