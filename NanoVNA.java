/*
 * This is the Basic of NanoVNA class
 * Some codes are refer to the https://github.com/ttrftech/NanoVNA/blob/master/python/nanovna.py
 */
package readVNA;

/**
 *
 * @author uqgzhu1
 */
import com.fazecast.jSerialComm.SerialPort;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import javax.swing.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

public class NanoVNA {
    private SerialPort serialPort;
    private String portDescriptor;
    private int points = 101;
    private double[] frequencies;
    
    public static String getPort() {
        final int VID = 0x0483;
        final int PID = 0x5740;

        for (SerialPort port : SerialPort.getCommPorts()) {
            if (port.getPortDescription().toLowerCase().contains("vna") ||
                (port.getVendorID() == VID && port.getProductID() == PID)) {
                return port.getSystemPortName();
            }
        }
        throw new RuntimeException("NanoVNA device not found");
    }
    public NanoVNA(String portDescriptor) {
        this.portDescriptor = portDescriptor;
    }

    public boolean open() {
        serialPort = SerialPort.getCommPort(portDescriptor);
        serialPort.setBaudRate(115200);
        serialPort.setNumDataBits(8);
        serialPort.setNumStopBits(SerialPort.ONE_STOP_BIT);
        serialPort.setParity(SerialPort.NO_PARITY);
        return serialPort.openPort();
    }

    public void close() {
        if (serialPort != null && serialPort.isOpen()) {
            serialPort.closePort();
        }
    }

    public void setFrequencies(double start, double stop, int points) {
        this.points = points;
        this.frequencies = new double[points];
        double step = (stop - start) / (points - 1);
        for (int i = 0; i < points; i++) {
            frequencies[i] = start + i * step;
        }
    }

    public String sendCommand(String command) throws IOException {
        if (!serialPort.isOpen()) {
            throw new IOException("Serial port is not open");
        }
        OutputStream out = serialPort.getOutputStream();
        out.write((command).getBytes());
        out.write('\r'); 
         out.flush();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException ex) {
            Logger.getLogger(NanoVNA.class.getName()).log(Level.SEVERE, null, ex);
        }
       
        
       // out.close();
         InputStreamReader inputStreamReader = new InputStreamReader(serialPort.getInputStream());
         Scanner reader = new Scanner(inputStreamReader);
           StringBuilder result = new StringBuilder();
         while(reader.hasNextLine()){
                        String line=reader.nextLine();
    
                        if (line.contains(command)) continue;
                        if (line.endsWith("ch>")) break;
                        
                        if (line.contains("ch>")) continue;
                        if (line.contains("NanoVNA")) continue;
                        result.append(line);
                        result.append("\n");
                        
                }
                while(reader.hasNextLine())
                    reader.nextLine();
         reader.reset();
         reader.close();
         inputStreamReader.close();

        return   result.toString();
    }

    private String readLine() throws IOException {
        InputStream in = serialPort.getInputStream();
        StringBuilder sb = new StringBuilder();
        int b;
        while ((b = in.read()) != -1) {
            char c = (char) b;
            if (c == '\n') break;
            sb.append(c);
        }
        return sb.toString();
    }

    public double[] getFrequencies() {
        return frequencies;
    }

    public List<double[]> scan() throws IOException {
        final int segmentLength = 101;
        List<Double> array0 = new ArrayList<>();
        List<Double> array1 = new ArrayList<>();

        double[] freqs = Arrays.copyOf(frequencies, frequencies.length);
        int offset = 0;

        while (offset < freqs.length) {
            int length = Math.min(segmentLength, freqs.length - offset);
            double segStart = freqs[offset];
            double segStop = freqs[offset + length - 1];

            //System.out.println("Segment: " + segStart + " to " + segStop + " (" + length + " points)");
            sendCommand("scan " + (long) segStart + " " + (long) segStop + " " + length);

            double[] segment0 = fetchArray(0);
            double[] segment1 = fetchArray(1);

            for (double val : segment0) array0.add(val);
            for (double val : segment1) array1.add(val);

            offset += segmentLength;
        }

        sendCommand("resume");
        return Arrays.asList(
            array0.stream().mapToDouble(Double::doubleValue).toArray(),
            array1.stream().mapToDouble(Double::doubleValue).toArray()
        );
    }
    
    public double[] fetchArray(int sel) throws IOException {
        String data=sendCommand("data " + sel);

        String[] lines = data.split("\\n");
        List<Double> reIm = new ArrayList<>();
        for (String line : lines) {
            if (!line.trim().isEmpty()) {
                for (String part : line.trim().split(" ")) {
                    reIm.add(Double.parseDouble(part));
                }
            }
        }
        double[] result = new double[reIm.size() / 2];
        for (int i = 0; i < result.length; i++) {
            double re = reIm.get(i * 2);
            double im = reIm.get(i * 2 + 1);
            result[i] = 20 * Math.log10(Math.sqrt(re * re + im * im));
        }
        return result;
    }

    public void plotLogMag(double[] magnitudes) {
        XYSeries series = new XYSeries("Log Magnitude");
        for (int i = 0; i < magnitudes.length && i < frequencies.length; i++) {
            series.add(frequencies[i], magnitudes[i]);
        }
        XYSeriesCollection dataset = new XYSeriesCollection(series);
        JFreeChart chart = ChartFactory.createXYLineChart(
                "Log Magnitude",
                "Frequency (Hz)",
                "Magnitude (dB)",
                dataset,
                PlotOrientation.VERTICAL,
                true,
                true,
                false
        );

        JFrame frame = new JFrame("NanoVNA Plot");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(new ChartPanel(chart));
        frame.pack();
        frame.setVisible(true);
    }
    
}
