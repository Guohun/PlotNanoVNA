/*
 * This is a demo to read the NanoVNA data and plot Mod of the signal
 * Some codes are refer to the https://github.com/ttrftech/NanoVNA/blob/master/python/nanovna.py
 */
package readVNA;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author uqgzhu1
 */
class NanoVNACommands {
    private NanoVNA vna;
    private List <Float>  _frequencies;

    public NanoVNACommands(NanoVNA vna) {
        this.vna = vna;
    }

    public void setSweep(long start, long stop) throws IOException {
        if (start > 0) {
            vna.sendCommand("sweep start " + start);
        }
        if (stop > 0) {
            vna.sendCommand("sweep stop " + stop);
        }
    }
    public void fetch_frequencies(){
        try {
            String data =vna.sendCommand("frequencies");
            this._frequencies=new ArrayList<Float>();
            for (String  line: data.split("\n")){
        
                if (!line.isEmpty())
                        this._frequencies.add(Float.valueOf(line));
            }
            
           } catch (IOException ex) {
            Logger.getLogger(NanoVNACommands.class.getName()).log(Level.SEVERE, null, ex);
        }
      }
    public void setFrequency(long freq) throws IOException {
        if (freq > 0) {
            vna.sendCommand("freq " + freq);
        }
    }

    public void setPort(int port) throws IOException {
        vna.sendCommand("port " + port);
    }

    public void setGain(int gain) throws IOException {
        vna.sendCommand("gain " + gain + " " + gain);
    }

    public void setOffset(int offset) throws IOException {
        vna.sendCommand("offset " + offset);
    }

    public void setPower(int power) throws IOException {
        vna.sendCommand("power " + power);
    }

    public void pause() throws IOException {
        vna.sendCommand("pause");
    }

    public void resume() throws IOException {
        vna.sendCommand("resume");
    }

    public void scan(double start, double stop, int points) throws IOException {
        vna.sendCommand("scan " + (long) start + " " + (long) stop + " " + points);
    }
    
    public static void main(String[] args) {
        //String port = "COM3"; // change to match your platform
        String port = NanoVNA.getPort();
            
        double startFreq = 1e6;
        double stopFreq = 900e6;
        int points = 101;
        int portIndex = 0;

        try {
            NanoVNA vna = new NanoVNA(port);
            if (!vna.open()) {
                System.err.println("Failed to open port: " + port);
                return;
            }

            NanoVNACommands commands = new NanoVNACommands(vna);
            vna.setFrequencies(startFreq, stopFreq, points);
            
            commands.setSweep((long)startFreq, (long)stopFreq);
            //commands.setPort(portIndex);
            commands.fetch_frequencies();
            //commands.scan(startFreq, stopFreq, points);
           
            double[] magnitude = vna.fetchArray(portIndex);
            commands.fetch_frequencies();
            vna.plotLogMag(magnitude);

            vna.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
