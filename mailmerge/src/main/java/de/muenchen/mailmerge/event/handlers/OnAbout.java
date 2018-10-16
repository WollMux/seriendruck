package de.muenchen.mailmerge.event.handlers;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.net.URL;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;

import de.muenchen.allg.itd51.wollmux.core.util.L;
import de.muenchen.mailmerge.MailMergeFehlerException;
import de.muenchen.mailmerge.MailMergeFiles;
import de.muenchen.mailmerge.MailMergeSingleton;
import de.muenchen.mailmerge.dialog.Common;

public class OnAbout extends BasicEvent
{
  private final JDialog dialog;

  public OnAbout()
  {
    Common.setLookAndFeelOnce();

    // non-modal dialog. Set 3rd param to true to make modal
    dialog = new JDialog((Frame) null, L.m("Info über LHM Seriendruck"), false);
    JPanel myPanel = new JPanel();
    myPanel.setLayout(new BoxLayout(myPanel, BoxLayout.Y_AXIS));
    myPanel.setBackground(Color.WHITE);
    dialog.setBackground(Color.WHITE);
    dialog.setContentPane(myPanel);
    JPanel imagePanel = new JPanel(new BorderLayout());
    URL imageURL = this.getClass().getClassLoader().getResource("mailmerge_klein.jpg");
    imagePanel.add(new JLabel(new ImageIcon(imageURL)), BorderLayout.CENTER);
    imagePanel.setOpaque(false);
    Box copyrightPanel = Box.createVerticalBox();
    copyrightPanel.setOpaque(false);
    Box hbox = Box.createHorizontalBox();
    hbox.add(imagePanel);
    hbox.add(copyrightPanel);
    myPanel.add(hbox);

    copyrightPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
    JLabel label = new JLabel(L.m("Seriendruck") + " " + MailMergeSingleton.getVersion());
    Font largeFont = label.getFont().deriveFont(15.0f);
    label.setFont(largeFont);
    copyrightPanel.add(label);
    label = new JLabel(L.m("Copyright (c) 2005-2016 Landeshauptstadt München"));
    label.setFont(largeFont);
    copyrightPanel.add(label);
    label = new JLabel(L.m("Lizenz: %1", "European Union Public License"));
    label.setFont(largeFont);
    copyrightPanel.add(label);
    label = new JLabel(L.m("Homepage: %1", "www.wollmux.org"));
    label.setFont(largeFont);
    copyrightPanel.add(label);

    myPanel.add(Box.createVerticalStrut(8));
    Box infoOuterPanel = Box.createHorizontalBox();
    infoOuterPanel.add(Box.createHorizontalStrut(8));
    JPanel infoPanel = new JPanel(new GridLayout(4, 1));
    infoOuterPanel.add(infoPanel);
    myPanel.add(infoOuterPanel);
    infoOuterPanel.add(Box.createHorizontalStrut(8));
    infoPanel.setOpaque(false);
    infoPanel.setBorder(BorderFactory.createTitledBorder(L.m("Info")));

    infoPanel.add(new JLabel(L.m("Seriendruck") + " " + MailMergeSingleton.getBuildInfo()));

    infoPanel
        .add(new JLabel(L.m("WollMux-Konfiguration:") + " " + MailMergeSingleton.getInstance().getConfVersionInfo()));

    infoPanel.add(new JLabel("DEFAULT_CONTEXT: " + MailMergeFiles.getDEFAULT_CONTEXT().toExternalForm()));

    myPanel.add(Box.createVerticalStrut(4));

    hbox = Box.createHorizontalBox();
    hbox.add(Box.createHorizontalGlue());
    hbox.add(new JButton(new AbstractAction(L.m("OK"))
    {
      private static final long serialVersionUID = 4527702807001201116L;

      @Override
      public void actionPerformed(ActionEvent e)
      {
        dialog.dispose();
      }
    }));
    hbox.add(Box.createHorizontalGlue());
    myPanel.add(hbox);

    myPanel.add(Box.createVerticalStrut(4));

    dialog.setBackground(Color.WHITE);
    myPanel.setBackground(Color.WHITE);
    dialog.pack();
    int frameWidth = dialog.getWidth();
    int frameHeight = dialog.getHeight();
    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    int x = screenSize.width / 2 - frameWidth / 2;
    int y = screenSize.height / 2 - frameHeight / 2;
    dialog.setLocation(x, y);
    dialog.setAlwaysOnTop(true);
  }

  @Override
  protected void doit() throws MailMergeFehlerException
  {
    dialog.setVisible(true);
  }

  @Override
  public String toString()
  {
    return this.getClass().getSimpleName() + "()";
  }
}