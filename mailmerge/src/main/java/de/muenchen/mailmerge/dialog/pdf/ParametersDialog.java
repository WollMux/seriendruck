package de.muenchen.mailmerge.dialog.pdf;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileFilter;

import com.sun.jna.platform.win32.WinReg;
import com.sun.star.lib.loader.RegistryAccess;
import com.sun.star.lib.loader.RegistryAccessException;

/**
 * Beschreibt den Dialog zur Einstellung der Optionen für das
 * PDF-Gesamtdokument
 *
 * @author Christoph Lutz (D-III-ITD-D101)
 */
public class ParametersDialog
{
  /**
   * Kommando-String, der dem closeActionListener übermittelt wird, wenn der
   * Dialog über den Drucken-Knopf geschlossen wird.
   */
  public static final String CMD_SUBMIT = "submit";

  /**
   * Kommando-String, der dem closeActionListener übermittelt wird, wenn der
   * Dialog über den Abbrechen oder "X"-Knopf geschlossen wird.
   */
  public static final String CMD_CANCEL = "cancel";

  /**
   * Enthält das Kommando für den PDF-Viewer, der im Druckdialog vorausgewählt
   * sein soll.
   */
  private static final String PDF_VIEWER_DEFAULT_COMMAND = "xdg-open";

  private JDialog dialog;

  private boolean isCanceled = false;

  private ActionListener closeActionListener;

  private JCheckBox duplexCheckBox;

  private JCheckBox outputFileCheckBox;

  private JCheckBox viewerCheckBox;

  private JTextField outputFileTextField;

  private JTextField viewerTextField;

  private JButton outputFileChooserButton;

  private JButton viewerChooserButton;

  private String currentPath;

  private WindowListener myWindowListener = new WindowAdapter()
  {
    @Override
    public void windowClosing(WindowEvent e)
    {
      abort(CMD_CANCEL);
    }
  };

  public ParametersDialog()
  {
  }

  /**
   * Konstruktor
   *
   * @param outputDir
   *          Pfad zum Verzeichnis der Ausgabedatei, der voreingestellt sein
   *          soll
   */
  public ParametersDialog(String outputDir)
  {
    currentPath = outputDir;
  }

  protected JCheckBox getOutputFileCheckBox()
  {
    return outputFileCheckBox;
  }

  protected JCheckBox getViewerCheckBox()
  {
    return viewerCheckBox;
  }

  protected JTextField getOutputFileTextField()
  {
    return outputFileTextField;
  }

  protected JTextField getViewerTextField()
  {
    return viewerTextField;
  }

  /**
   * Startet den Dialog und kehrt erst wieder zurück, wenn der Dialog beendet
   * ist; selbstverständlich wird dabei auf die Thread-Safety bezüglich des
   * EDT geachtet.
   *
   * @param isInitialDialog
   *          Entscheidet ob der Dialog das erste Mal angezeigt wird, oder ein
   *          weiteres Mal, um Fehlentscheidungen (z. B. Datei ohne
   *          Schreibberechtigung) zu korrigieren.
   *
   * @param outputFilename
   *          Vorauswahl zum Namen der Ausgabedatei, falls isInitialDialog
   *          <code>false</false> ist
   * @param pdfViewerCommand
   *          Vorauswahl zum Pdf-Viewer Programm, falls isInitialDialog
   *          <code>false</false> ist
   * @param duplex
   *          Vorauswahl zur Einstellung "Duplexseiten benötigt", falls
   *          isInitialDialog <code>false</false> ist
   * @author Christoph Lutz (D-III-ITD-D101)
   */
  public void showDialog(boolean isInitialDialog, String outputFilename,
      String pdfViewerCommand, boolean duplex)
  {
    final boolean[] lock = new boolean[] { true };
    final boolean isInitial = isInitialDialog;
    final boolean duplexRequired = duplex;
    final String outputName = outputFilename;
    final String pdfViewer = pdfViewerCommand;
    this.closeActionListener = new ActionListener()
    {
      @Override
      public void actionPerformed(ActionEvent e)
      {
        synchronized (lock)
        {
          lock[0] = false;
          lock.notify();
        }
      }
    };
    SwingUtilities.invokeLater(new Runnable()
    {
      @Override
      public void run()
      {
        createGUI(isInitial, outputName, pdfViewer, duplexRequired);
      }
    });
    synchronized (lock)
    {
      while (lock[0])
        try
        {
          lock.wait();
        } catch (InterruptedException e1)
        {
          isCanceled = true;
          return;
        }
    }
  }

  /**
   * Liefert true, wenn der Dialog mit Abbrechen oder dem X-Button beendet
   * wurde.
   *
   * @return
   *
   * @author Christoph Lutz (D-III-ITD-D101)
   */
  public boolean isCanceled()
  {
    return isCanceled;
  }

  /**
   * Liefert das File unter dem das Ausgabedokument gespeichert werden soll
   * zurück, oder null, wenn die Checkbox "speichern unter..." nicht
   * angekreuzt ist.
   *
   * @return File-Objekt auf das Ausgabedokument; kann auch null sein, wenn
   *         kein Ausgabedokument definiert wurde.
   *
   * @author Christoph Lutz (D-III-ITD-D101)
   */
  public File getOutputFile()
  {
    if (outputFileCheckBox.isSelected())
      return new File(outputFileTextField.getText());
    return null;
  }

  /**
   * Liefert den Wert der Checkbox "Leerseiten für Duplexdruck" zurück.
   *
   * @return true, wenn das Duplexdrucken erwünscht ist, ansonsten false.
   *
   * @author Christoph Lutz (D-III-ITD-D101)
   */
  public boolean isDuplexPrintRequired()
  {
    return duplexCheckBox.isSelected();
  }

  /**
   * Liefert das Kommando zurück, mit dem der PDF-Viewer aufgerufen werden
   * soll, oder null, wenn nicht.
   *
   * @return Den Kommandostring für den gewünschten PDF-Viewer oder null, wenn
   *         die zugehörige Option nicht selektiert ist.
   *
   * @author Christoph Lutz (D-III-ITD-D101)
   */
  public String getPDFViewerCmd()
  {
    if (viewerCheckBox.isSelected())
      return viewerTextField.getText();
    return null;
  }

  /**
   * Baut das GUI auf.
   *
   * @param isInitialDialog
   *          Entscheidet ob der Dialog das erste Mal angezeigt wird, oder ein
   *          weiteres Mal, um Fehlentscheidungen (z. B. Datei ohne
   *          Schreibberechtigung) zu korrigieren.
   *
   * @param outputFilename
   *          Vorauswahl zum Namen der Ausgabedatei, falls isInitialDialog
   *          <code>false</false> ist
   * @param pdfViewerCommand
   *          Vorauswahl zum Pdf-Viewer Programm, falls isInitialDialog
   *          <code>false</false> ist
   * @param duplexRequired
   *          Vorauswahl zur Einstellung "Duplexseiten benötigt", falls
   *          isInitialDialog <code>false</false> ist
   */
  private void createGUI(boolean isInitialDialog, String outputFilename,
      String pdfViewer, boolean duplexRequired)
  {
    dialog = new JDialog();
    dialog.setTitle("PDF-Gesamtdokument");
    dialog.addWindowListener(myWindowListener);
    JPanel panel = new JPanel(new BorderLayout());
    panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
    dialog.add(panel);

    Box optionBox = Box.createVerticalBox();
    Box vbox = Box.createVerticalBox();
    vbox.setBorder(BorderFactory.createTitledBorder(
        BorderFactory.createRaisedBevelBorder(),
        "Einstellungen für die Dokumenterstellung"));
    optionBox.add(vbox);
    optionBox.add(Box.createVerticalStrut(10));
    Box hbox;

    createEmptySite(vbox);

    createDocumentSave(vbox);

    createDocumentView(vbox);

    panel.add(optionBox, BorderLayout.LINE_START);

    hbox = createButtons(isInitialDialog);
    panel.add(hbox, BorderLayout.SOUTH);

    dialog.setVisible(false);
    dialog.setAlwaysOnTop(true);
    dialog.pack();
    int frameWidth = dialog.getWidth();
    int frameHeight = dialog.getHeight();
    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    int x = screenSize.width / 2 - frameWidth / 2;
    int y = screenSize.height / 2 - frameHeight / 2;
    dialog.setLocation(x, y);
    dialog.setResizable(false);
    dialog.setVisible(true);
    if (!isInitialDialog)
    {
      duplexCheckBox.setEnabled(false);
      duplexCheckBox.setSelected(duplexRequired);
      viewerCheckBox.setEnabled(false);
      viewerTextField.setEnabled(false);
      viewerChooserButton.setEnabled(false);
      outputFileCheckBox.setSelected(true);
      outputFileTextField.setEnabled(outputFileCheckBox.isSelected());
      outputFileChooserButton.setEnabled(outputFileCheckBox.isSelected());
      if (outputFilename != null)
      {
        outputFileTextField.setText(outputFilename);
      }
      if (pdfViewer != null)
      {
        viewerCheckBox.setSelected(true);
        viewerTextField.setText(pdfViewer);
      } else
      {
        viewerCheckBox.setSelected(false);
      }
    }
  }

  private Box createButtons(boolean isInitialDialog)
  {
    JButton button;
    Box hbox = Box.createHorizontalBox();
    if (isInitialDialog)
    {
      button = new JButton("Abbrechen");
      button.addActionListener(new ActionListener()
      {
        @Override
        public void actionPerformed(ActionEvent e)
        {
          abort(CMD_CANCEL);
        }
      });
      hbox.add(button);
    }
    hbox.add(Box.createHorizontalGlue());
    if (isInitialDialog)
    {
      button = new JButton("Drucken");
    } else
    {
      button = new JButton("Ok");
    }
    button.addActionListener(new PrintActionListener(this));
    hbox.add(button);
    return hbox;
  }

  private void createDocumentView(Box vbox)
  {
    Box hbox = Box.createHorizontalBox();
    viewerCheckBox = new JCheckBox("Dokument gleich betrachten mit");
    viewerCheckBox.setSelected(true);
    hbox.add(viewerCheckBox);
    hbox.add(Box.createHorizontalGlue());
    vbox.add(hbox);

    hbox = Box.createHorizontalBox();
    hbox.add(Box.createHorizontalStrut(22));
    viewerTextField = new JTextField(getDefaultPDFViewer());
    hbox.add(viewerTextField);
    hbox.add(Box.createHorizontalStrut(10));
    viewerChooserButton = new JButton("...");
    viewerChooserButton.addActionListener(new ActionListener()
    {
      @Override
      public void actionPerformed(ActionEvent e)
      {
        showViewerFileChooser();
      }
    });
    viewerCheckBox.addActionListener(new ActionListener()
    {
      @Override
      public void actionPerformed(ActionEvent arg0)
      {
        viewerTextField.setEnabled(viewerCheckBox.isSelected());
        viewerChooserButton.setEnabled(viewerCheckBox.isSelected());
      }
    });
    hbox.add(viewerChooserButton);
    hbox.add(Box.createHorizontalStrut(10));
    hbox.add(Box.createHorizontalGlue());
    vbox.add(hbox);
    vbox.add(Box.createVerticalStrut(10));
  }

  private void createDocumentSave(Box vbox)
  {
    Box hbox = Box.createHorizontalBox();
    outputFileCheckBox = new JCheckBox("Dokument speichern unter");
    outputFileCheckBox.setSelected(false);
    hbox.add(outputFileCheckBox);
    hbox.add(Box.createHorizontalGlue());
    vbox.add(hbox);

    hbox = Box.createHorizontalBox();
    hbox.add(Box.createHorizontalStrut(22));
    outputFileTextField = new JTextField("");
    outputFileTextField.setEnabled(false);
    hbox.add(outputFileTextField);
    hbox.add(Box.createHorizontalStrut(10));
    outputFileChooserButton = new JButton("...");
    outputFileChooserButton.setEnabled(false);
    outputFileChooserButton.addActionListener(new ActionListener()
    {
      @Override
      public void actionPerformed(ActionEvent e)
      {
        showOutputFilePicker();
      }
    });
    hbox.add(outputFileChooserButton);
    outputFileCheckBox.addActionListener(new ActionListener()
    {
      @Override
      public void actionPerformed(ActionEvent arg0)
      {
        outputFileTextField.setEnabled(outputFileCheckBox.isSelected());
        outputFileChooserButton.setEnabled(outputFileCheckBox.isSelected());
      }
    });
    hbox.add(Box.createHorizontalStrut(10));
    hbox.add(Box.createHorizontalGlue());
    vbox.add(hbox);
    vbox.add(Box.createVerticalStrut(10));
  }

  private void createEmptySite(Box vbox)
  {
    Box hbox = Box.createHorizontalBox();
    duplexCheckBox = new JCheckBox("Leerseiten für Duplexdruck einfügen");
    hbox.add(duplexCheckBox);
    hbox.add(Box.createHorizontalGlue());
    vbox.add(hbox);

    hbox = Box.createHorizontalBox();
    hbox.add(Box.createHorizontalStrut(22));
    hbox.add(new JLabel(
        "<html>Leerseiten einfügen, damit alle Ausdrucke mit ungeraden Seitennummern<br/>beginnen. Dies wird benötigt, wenn das Dokument später per Duplexdruck<br/>gedruckt werden soll.</html>"));
    hbox.add(Box.createHorizontalStrut(10));
    hbox.add(Box.createHorizontalGlue());
    vbox.add(hbox);
    vbox.add(Box.createVerticalStrut(10));
  }

  /**
   * Liefert den (systemabhängigen) PDF-Viewer, der als Vorbelegung im Dialog
   * angezeigt werden soll; Dabei wird unter Windows in der Registry nach dem
   * Default-Open-Kommando für .pdf Dateien gesucht; wird hier nichts
   * gefunden, so liefert die Methode den konstanten Wert
   * PDF_VIEWER_DEFAULT_COMMAND zurück (insbesondere für Linux geeignet).
   *
   * @author Christoph Lutz (D-III-ITD-D101)
   */
  private static String getDefaultPDFViewer()
  {
    try
    {
      String progId = RegistryAccess
          .getStringValueFromRegistry(WinReg.HKEY_CLASSES_ROOT, ".pdf", "");
      if (progId != null)
      {
        String cmd = RegistryAccess.getStringValueFromRegistry(
            WinReg.HKEY_CLASSES_ROOT, "" + progId + "\\shell\\open\\command", "");
        if (cmd != null)
        {
          // cmd kann sein z.B. '"C:\Programme...\acrobat.exe" "%1"' oder
          // 'acrobat %1'
          Matcher m = Pattern.compile("(?:\"([^\"]*)\"|([^\\s]*))\\s*")
              .matcher(cmd);
          if (m.find())
          {
            if (m.group(1) != null)
              return m.group(1);
            if (m.group(2) != null)
              return m.group(2);
          }
        }
      }
    } catch (RegistryAccessException e)
    {
      // return default pdf viewer
    }
    return PDF_VIEWER_DEFAULT_COMMAND;
  }

  protected void printButtonPressed()
  {
    abort(CMD_SUBMIT);
  }

  protected void abort(String commandStr)
  {
    if (CMD_CANCEL.equals(commandStr))
      isCanceled = true;

    /*
     * Wegen folgendem Java Bug (WONTFIX)
     * http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4259304 sind die
     * folgenden 3 Zeilen nötig, damit der Dialog gc'ed werden kann. Die
     * Befehle sorgen dafür, dass kein globales Objekt (wie z.B. der
     * Keyboard-Fokus-Manager) indirekt über den JFrame den Dialog kennt.
     */
    if (dialog != null)
    {
      dialog.removeWindowListener(myWindowListener);
      dialog.getContentPane().remove(0);
      dialog.setJMenuBar(null);

      dialog.dispose();
      dialog = null;
    }

    if (closeActionListener != null)
      closeActionListener
          .actionPerformed(new ActionEvent(this, 0, commandStr));
  }

  /**
   * Startet den FileChooser für die Auswahl des zu speichernden Dokuments und
   * merkt sich dabei das zuletzt ausgewählte Verzeichnis, um beim nächsten
   * Öffnen des Dialogs erneut in diesem Verzeichnis starten zu können.
   *
   * @return Der ausgewählte Name für die Auswahl des Dokuments, ggf.
   *         erweitert um die Endung ".pdf"
   *
   * @author Christoph Lutz (D-III-ITD-D101)
   */
  private String showOutputFilePicker()
  {
    String selectedFile = null;
    JFileChooser fc;
    if (currentPath != null)
      fc = new JFileChooser(currentPath);
    else
      fc = new JFileChooser();
    FileFilter filter = new FileFilter()
    {
      @Override
      public String getDescription()
      {
        return "PDF-Dokumente";
      }

      @Override
      public boolean accept(File f)
      {
        if (f != null)
        {
          if (f.getName().toLowerCase().endsWith(".pdf"))
            return true;
          if (f.isDirectory())
            return true;
        }
        return false;
      }
    };
    fc.setFileFilter(filter);
    fc.setDialogTitle("Dokument speichern unter...");
    fc.setMultiSelectionEnabled(false);

    int ret = fc.showSaveDialog(dialog);

    if (ret == JFileChooser.APPROVE_OPTION)
    {
      currentPath = fc.getSelectedFile().getParent();
      String fname = fc.getSelectedFile().getAbsolutePath();
      if (!fname.toLowerCase().endsWith(".pdf"))
        fname = fname + ".pdf";
      outputFileTextField.setText(fname);
      selectedFile = fname;
    }
    return selectedFile;
  }

  /**
   * Startet den FileChooser für die Auswahl des PDF-Viewers
   *
   * @author Christoph Lutz (D-III-ITD-D101)
   */
  private void showViewerFileChooser()
  {
    JFileChooser fc = new JFileChooser("/");
    fc.setDialogTitle("PDF-Betrachter auswählen");
    fc.setMultiSelectionEnabled(false);
    int ret = fc.showOpenDialog(dialog);
    if (ret == JFileChooser.APPROVE_OPTION)
      viewerTextField.setText(fc.getSelectedFile().getAbsolutePath());
  }
}
