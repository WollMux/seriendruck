package de.muenchen.mailmerge.dialog.mailmerge;

import java.awt.Dimension;
import java.awt.Toolkit;

import javax.swing.Box;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import de.muenchen.allg.itd51.wollmux.core.util.L;
import de.muenchen.allg.itd51.wollmux.core.util.Logger;
import de.muenchen.mailmerge.dialog.Common;

public class MailMergeProgressWindow
{
  private JFrame myFrame;

  private JLabel countLabel;

  private int count = 0;

  private int maxcount;

  public MailMergeProgressWindow(final int maxcount)
  {
    this.maxcount = maxcount;
    try
    {
      SwingUtilities.invokeAndWait(new Runnable()
      {
        @Override
        public void run()
        {
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
        }
      });
    }
    catch (Exception x)
    {
      Logger.error(x);
    }
    ;
  }

  public void makeProgress()
  {
    try
    {
      SwingUtilities.invokeLater(new Runnable()
      {
        @Override
        public void run()
        {
          ++count;
          countLabel.setText("" + count);
          if (maxcount > 0)
            myFrame.setTitle("" + Math.round(100 * (double) count / maxcount)
              + "%");
        }
      });
    }
    catch (Exception x)
    {
      Logger.error(x);
    }
    ;
  }

  public void close()
  {
    try
    {
      SwingUtilities.invokeLater(new Runnable()
      {
        @Override
        public void run()
        {
          myFrame.dispose();
        }
      });
    }
    catch (Exception x)
    {
      Logger.error(x);
    }
    ;
  }
}