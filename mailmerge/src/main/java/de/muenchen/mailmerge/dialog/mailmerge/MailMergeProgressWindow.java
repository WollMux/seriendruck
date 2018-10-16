package de.muenchen.mailmerge.dialog.mailmerge;

import java.awt.Dimension;
import java.awt.Toolkit;

import javax.swing.Box;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.muenchen.allg.itd51.wollmux.core.util.L;
import de.muenchen.mailmerge.dialog.Common;

public class MailMergeProgressWindow
{

  private static final Logger LOGGER = LoggerFactory.getLogger(MailMergeProgressWindow.class);

  private JFrame myFrame;

  private JLabel countLabel;

  private int count = 0;

  private int maxcount;

  public MailMergeProgressWindow(final int maxcount)
  {
    this.maxcount = maxcount;
    try
    {
      SwingUtilities.invokeAndWait(() -> {
        myFrame = new JFrame(L.m("Seriendruck"));
        Common.setWollMuxIcon(myFrame);
        Box vbox = Box.createVerticalBox();
        myFrame.getContentPane().add(vbox);
        Box hbox = Box.createHorizontalBox();
        vbox.add(hbox);
        hbox.add(Box.createHorizontalStrut(5));
        hbox.add(new JLabel(L.m("Verarbeite Dokument")));
        hbox.add(Box.createHorizontalStrut(5));
        countLabel = new JLabel("   -");
        hbox.add(countLabel);
        hbox.add(new JLabel(" / " + maxcount + "    "));
        hbox.add(Box.createHorizontalStrut(5));
        myFrame.setAlwaysOnTop(true);
        myFrame.pack();
        int frameWidth = myFrame.getWidth();
        int frameHeight = myFrame.getHeight();
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int x = screenSize.width / 2 - frameWidth / 2;
        int y = screenSize.height / 2 - frameHeight / 2;
        myFrame.setLocation(x, y);
        myFrame.setVisible(true);
      });
    } catch (Exception x)
    {
      LOGGER.error("", x);
    }
  }

  public void makeProgress()
  {
    try
    {
      SwingUtilities.invokeLater(() -> {
        ++count;
        countLabel.setText("" + count);
        if (maxcount > 0)
          myFrame.setTitle("" + Math.round(100 * (double) count / maxcount) + "%");
      });
    } catch (Exception x)
    {
      LOGGER.error("", x);
    }
  }

  public void close()
  {
    try
    {
      SwingUtilities.invokeLater(() -> myFrame.dispose());
    } catch (Exception x)
    {
      LOGGER.error("", x);
    }
  }
}