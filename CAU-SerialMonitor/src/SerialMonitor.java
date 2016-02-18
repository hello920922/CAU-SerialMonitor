import gnu.io.CommPortIdentifier;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;
import gnu.io.UnsupportedCommOperationException;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.TooManyListenersException;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;


public class SerialMonitor extends JFrame implements ComponentListener, SerialPortEventListener, ActionListener, KeyListener{
    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    private final String TITLE = "CAU NSL Serial Monitor";

    /* The Variables for Layout */
    private final int LINE_WIDTH = 3;
    private final int FRAME_WIDTH = 600;
    private final int FRAME_HEIGHT = 500;
    private final int MARGIN_HEIGHT = 39;
    private final int MARGIN_WIDTH = 19;
    private final int COMBO_HEIGHT = 20;

    /* The Variables for PortList */
    private final int LIST_WIDTH = 150;
    private final int PORT_START_Y = 50;
    private final int PORT_HEIGHT = 50;
    private final int COMBO_BAUDRATE_WIDTH = 65;
    
    /* The Variables for Output Field */
    private final int START_FIELD = LINE_WIDTH + LIST_WIDTH;
    private final int FIELD_HEIGHT = 20;
    private final int BUTTON_WIDTH = 85;
    private final int INPUT_TEXT_WIDTH = FRAME_WIDTH - START_FIELD - LINE_WIDTH * 2 - BUTTON_WIDTH - MARGIN_WIDTH;
    private final int TEXT_AREA_WIDTH = FRAME_WIDTH - START_FIELD - LINE_WIDTH - MARGIN_WIDTH;
    private final int TEXT_AREA_HEIGTH = FRAME_HEIGHT - LINE_WIDTH * 2 - FIELD_HEIGHT * 2 - MARGIN_HEIGHT; 
    private final int COMBO_LINE_ENDING_WIDTH = 110;
    private final int BUTTON_SAVE_PATH_WIDTH = 90;
    
    /* The Variables for Serial Communication */
    private final int CONNECTION_TIMEOUT = 2000;

    private JPanel portPane;
    private JPanel portListPane;
    private JPanel monitorPane;
    private JComboBox<Integer> baudrateList;
    private JComboBox<String> lineEndingList;
    private JButton send;
    private JButton pause;
    private JButton path;
    private JTextField inputField;
    private JLabel connectedPort;
    private JPanel textAreaPane;
    private JTextArea textArea;
    
    private SerialPort serialPort;
    private InputStream in;
    private OutputStream out;
    private FileWriter fileWriter;
    
    private DiscoveryPortThread discoveryPortThread;
    private boolean isConnected;
    private boolean isPaused;
    private String lineEndingChar;
    private String[] lineEndingChars = {"", "\n", "\r", "\n\r"};
    private File directory;

    public SerialMonitor() {
        /* Default Setting for Frame */
        setTitle(TITLE);
        setLayout(null);
        setBounds(200, 300, FRAME_WIDTH, FRAME_HEIGHT);
        setMinimumSize(getSize());
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        addComponentListener(this);

        /* Default Setting for Serial Port List Panel */
        portPane = new JPanel(null);
        portPane.setBounds(0, 0, LIST_WIDTH, FRAME_HEIGHT-MARGIN_HEIGHT);
        portPane.setBackground(Color.WHITE);
        add(portPane);
        
        portListPane = new JPanel(null);
        portListPane.setBounds(0, PORT_START_Y, LIST_WIDTH, FRAME_HEIGHT-MARGIN_HEIGHT-PORT_START_Y);
        portListPane.setBackground(Color.WHITE);
        portPane.add(portListPane);
        
        /* Default Setting for Baudrate List */
        JLabel baudrateLabel = new JLabel("Baud Rate : " );
        baudrateLabel.setBounds(0, 0, LIST_WIDTH-COMBO_BAUDRATE_WIDTH, COMBO_HEIGHT);
        baudrateLabel.setHorizontalAlignment(JLabel.CENTER);
        portPane.add(baudrateLabel);
        
        Integer[] baudrate = {300, 1200, 2400, 4800, 9600, 19200, 38400, 57600, 74880, 115200, 230400, 250000};
        baudrateList = new JComboBox<>(baudrate);
        baudrateList.setBounds(LIST_WIDTH-COMBO_BAUDRATE_WIDTH, 0, COMBO_BAUDRATE_WIDTH, COMBO_HEIGHT);
        baudrateList.addActionListener(this);
        portPane.add(baudrateList);

        /* Default Setting for Serial Port List */
        JLabel portListLabel = new JLabel("---- PORT LIST ----");
        portListLabel.setBounds(0, COMBO_HEIGHT, LIST_WIDTH, PORT_START_Y-COMBO_HEIGHT);
        portListLabel.setHorizontalAlignment(JLabel.CENTER);
        portPane.add(portListLabel);
        
        /* Default Setting for Other Panel */
        monitorPane = new JPanel(null);
        monitorPane.setBounds(START_FIELD, 0, FRAME_WIDTH-MARGIN_WIDTH-START_FIELD, FRAME_HEIGHT-MARGIN_HEIGHT);
        add(monitorPane);
        
        /* Default Setting for Input Field */
        send = new JButton("Send");
        send.setBounds(INPUT_TEXT_WIDTH + LINE_WIDTH, 0, BUTTON_WIDTH, FIELD_HEIGHT);
        send.addActionListener(this);
        monitorPane.add(send);
        
        inputField = new JTextField();
        inputField.setBounds(0, 0, INPUT_TEXT_WIDTH, FIELD_HEIGHT);
        inputField.addKeyListener(this);
        monitorPane.add(inputField);
        
        connectedPort = new JLabel("Not Connected");
        connectedPort.setBounds(0, FIELD_HEIGHT+LINE_WIDTH+2, 160, FIELD_HEIGHT);
        monitorPane.add(connectedPort);
        
        pause = new JButton("Pause");
        pause.setBounds(INPUT_TEXT_WIDTH + LINE_WIDTH, FIELD_HEIGHT+LINE_WIDTH, BUTTON_WIDTH, FIELD_HEIGHT);
        pause.addActionListener(this);
        monitorPane.add(pause);
        
        path = new JButton("SavePath");
        path.setBounds(INPUT_TEXT_WIDTH - COMBO_LINE_ENDING_WIDTH - BUTTON_SAVE_PATH_WIDTH - LINE_WIDTH, FIELD_HEIGHT+LINE_WIDTH, BUTTON_SAVE_PATH_WIDTH, FIELD_HEIGHT);
        path.addActionListener(this);
        monitorPane.add(path);
        
        String[] lineEnding = {"No line ending", "New line", "Carriage return", "Both NL & CR"};
        lineEndingList = new JComboBox<>(lineEnding);
        lineEndingList.setBounds(INPUT_TEXT_WIDTH - COMBO_LINE_ENDING_WIDTH, FIELD_HEIGHT+LINE_WIDTH, COMBO_LINE_ENDING_WIDTH, FIELD_HEIGHT);
        lineEndingList.addActionListener(this);
        lineEndingChar = lineEndingChars[0];
        monitorPane.add(lineEndingList);
        
        /* Default Setting for Text Area */
        textArea = new JTextArea();
        textAreaPane = new JPanel(new BorderLayout());
        JScrollPane textAreaScrollPane = new JScrollPane(textArea);
        textAreaScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        textAreaScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        textArea.setLineWrap(true);
        textArea.setEditable(false);
        textAreaPane.setBounds(0, FIELD_HEIGHT*2+LINE_WIDTH*2, TEXT_AREA_WIDTH, TEXT_AREA_HEIGTH);
        textAreaPane.add(textAreaScrollPane, BorderLayout.CENTER);
        
        monitorPane.add(textAreaPane);
        
        discoveryPortThread = new DiscoveryPortThread();
        discoveryPortThread.start();

        setVisible(true);
    }
    
    class DiscoveryPortThread extends Thread{
        @Override
        public void run(){
            while(true){
                try{Thread.sleep(1000);}
                catch(Exception e){e.printStackTrace();}
                
                if(!isConnected)
                    printList();
            }
        }
        
        public void printList(){
            portListPane.removeAll();
            Enumeration<CommPortIdentifier> portList = CommPortIdentifier.getPortIdentifiers();
            int cnt = 0;
            while(portList.hasMoreElements()){
                CommPortIdentifier port = portList.nextElement();
                if(port.getPortType() == CommPortIdentifier.PORT_SERIAL){
                    JButton serialPortButton = new JButton(port.getName());
                    serialPortButton.setBounds(0, (cnt++)*PORT_HEIGHT, LIST_WIDTH, PORT_HEIGHT);
                    serialPortButton.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            serialConnect(port);
                        }
                    });
                    portListPane.add(serialPortButton);
                }
            }
            portListPane.repaint();
        }
    }
    
    public void serialConnect(CommPortIdentifier port){
        System.out.println("Try to connect with " + port.getName());
        try {
            if(serialPort != null){
                try {
                    in.close();
                    out.close();
                    fileWriter.close();
                    serialPort.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                serialPort = null;
            }
            serialPort = (SerialPort)port.open(port.getName(), CONNECTION_TIMEOUT);
            try {
                serialPort.addEventListener(this);
                serialPort.notifyOnDataAvailable(true);
                serialPort.setSerialPortParams((Integer)baudrateList.getSelectedItem(), SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);

                isConnected = true;
                System.out.println("Success to connect with " + port.getName());
                
                connectedPort.setText("Connected with " + port.getName());
                portListPane.removeAll();
                JButton disconnect = new JButton("Disconnect");
                disconnect.setBounds(0, 0, LIST_WIDTH, PORT_HEIGHT);
                disconnect.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        System.out.println("Disconnect");
                        connectedPort.setText("Not Connected");
                        isConnected = false;
                        isPaused = false;
                        pause.setText("Pause");
                        if(serialPort != null){
                            try {
                                in.close();
                                out.close();
                                fileWriter.close();
                                serialPort.close();
                            } catch (IOException ex) {
                                ex.printStackTrace();
                            }
                            serialPort = null;
                            textArea.setText("");
                        }
                        discoveryPortThread.printList();
                    }
                });
                portListPane.add(disconnect);
                portListPane.repaint();
                
                in = serialPort.getInputStream();
                out = serialPort.getOutputStream();
                fileWriter = makeFile();
            } catch (TooManyListenersException e) {
                serialPort.close();
                serialPort = null;
                e.printStackTrace();}
            catch (UnsupportedCommOperationException e) { e.printStackTrace(); }
            catch (IOException e){e.printStackTrace(); }
        } catch (PortInUseException e) {
            JOptionPane.showMessageDialog(null, port.getName()+" is already in use.", "Connection Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private FileWriter makeFile(){
        FileWriter writer = null;
        String filename = "log-";
        filename += new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        filename += ".txt";
        File file = null;
        if(directory == null){
            directory = new File("log");
            if(!directory.exists())
                directory.mkdir();
        }
        file = new File(directory,filename);
        
        try {
            writer = new FileWriter(file, true);
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        return writer;
    }

    @Override
    public void serialEvent(SerialPortEvent e) {
        if(e.getEventType() == SerialPortEvent.DATA_AVAILABLE){
            int c;
            try {
                String read = "";
                while( (c = in.read()) != -1 && isConnected && !isPaused){
                    read = String.valueOf((char)c); 
                    textArea.append(read);
                    fileWriter.write(read);
                    fileWriter.flush();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    @Override
    public void componentResized(ComponentEvent e) {
        /* Modify Port List Panel Components */
        portPane.setSize(LIST_WIDTH, getHeight()-MARGIN_HEIGHT);
        portListPane.setSize(LIST_WIDTH, getHeight()-MARGIN_HEIGHT-PORT_START_Y);
        
        /* Modify Monitor Panel Components */
        monitorPane.setSize(getWidth()-MARGIN_WIDTH-START_FIELD, getHeight()-MARGIN_HEIGHT);
        inputField.setSize(monitorPane.getWidth()-BUTTON_WIDTH-LINE_WIDTH-LINE_WIDTH, FIELD_HEIGHT);
        send.setLocation(monitorPane.getWidth()-BUTTON_WIDTH-LINE_WIDTH, 0);
        pause.setLocation(monitorPane.getWidth()-BUTTON_WIDTH-LINE_WIDTH, FIELD_HEIGHT+LINE_WIDTH);
        path.setLocation(monitorPane.getWidth()-BUTTON_WIDTH-COMBO_LINE_ENDING_WIDTH-LINE_WIDTH-LINE_WIDTH-LINE_WIDTH-BUTTON_SAVE_PATH_WIDTH, FIELD_HEIGHT+LINE_WIDTH);
        lineEndingList.setLocation(monitorPane.getWidth()-BUTTON_WIDTH-COMBO_LINE_ENDING_WIDTH-LINE_WIDTH-LINE_WIDTH, FIELD_HEIGHT+LINE_WIDTH);
        textAreaPane.setSize(monitorPane.getWidth() - LINE_WIDTH, getHeight() - LINE_WIDTH * 2 - FIELD_HEIGHT * 2 - MARGIN_HEIGHT);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if(e.getSource() == send){
            if(isConnected){
                try {
                    String line = inputField.getText() + lineEndingChar;
                    out.write(line.getBytes());
                    inputField.setText("");
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
        else if(e.getSource() == pause){
            if(isConnected){
                isPaused = !isPaused;
                if(isPaused) pause.setText("Resume");
                else pause.setText("Pause");
            }
        }
        else if(e.getSource() == lineEndingList){
            lineEndingChar = lineEndingChars[lineEndingList.getSelectedIndex()];
        }
        else if(e.getSource() == baudrateList && isConnected){
            try {
                serialPort.setSerialPortParams((Integer)baudrateList.getSelectedItem(), SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
            } catch (UnsupportedCommOperationException ex) {
                ex.printStackTrace();
            }
        }
        else if(e.getSource() == path){
            if(isConnected){
                JOptionPane.showMessageDialog(null, "Please disconnect first.", "Connection Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            JFileChooser chooser = new JFileChooser("./");
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            int option = chooser.showOpenDialog(null);
            if(option == JFileChooser.APPROVE_OPTION) directory = chooser.getSelectedFile();
            else directory = null;
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if(e.getKeyCode() == KeyEvent.VK_ENTER){
            if(isConnected){
                try {
                    String line = inputField.getText() + lineEndingChar;
                    out.write(line.getBytes());
                    inputField.setText("");
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    @Override
    public void componentHidden(ComponentEvent e) {}
    @Override
    public void componentMoved(ComponentEvent e) {}
    @Override
    public void componentShown(ComponentEvent e) {}
    @Override
    public void keyReleased(KeyEvent e) {}
    @Override
    public void keyTyped(KeyEvent e) {}

    public static void main(String [] args){
        new SerialMonitor();
    }

}
