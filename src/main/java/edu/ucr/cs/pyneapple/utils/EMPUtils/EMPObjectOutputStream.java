package edu.ucr.cs.pyneapple.utils.EMPUtils;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

public class EMPObjectOutputStream extends ObjectOutputStream {
    public EMPObjectOutputStream(OutputStream out) throws IOException {
        super(out);
    }
    EMPObjectOutputStream() throws IOException{
        super();
    }

    @Override
    protected void writeStreamHeader() throws IOException{reset();}
}
