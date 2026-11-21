package socket.GUI;

import javax.swing.*;
import java.awt.*;

public class ProgressDialog extends JDialog {
    private JProgressBar progressBar;

    public ProgressDialog(JFrame parent,String title){
        super(parent,title,true);
        setSize(300,100);
        setLocationRelativeTo(parent);
        setLayout(new FlowLayout(FlowLayout.CENTER,20,20));

        progressBar= new JProgressBar(0,100);
        progressBar.setPreferredSize(new Dimension(250,25));
        progressBar.setStringPainted(true);
        add(progressBar);
    }
    public void updateProgress(int percent){
        progressBar.setValue(percent);
    }
}
