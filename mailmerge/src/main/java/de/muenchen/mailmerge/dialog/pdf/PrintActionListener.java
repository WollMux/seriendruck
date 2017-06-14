package de.muenchen.mailmerge.dialog.pdf;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

public class PrintActionListener implements ActionListener
{

  private ParametersDialog dialog;

  public PrintActionListener(ParametersDialog dialog)
  {
    this.dialog = dialog;
  }

  @Override
  public void actionPerformed(ActionEvent e)
  {
    if(checkFile() || checkViewer())
    {
      return;
    }

    dialog.printButtonPressed();
  }

  private boolean checkFile()
  {
    if (!dialog.getOutputFileCheckBox().isSelected())
      return false;

    if (dialog.getOutputFileTextField().getText() == null
            || dialog.getOutputFileTextField().getText().trim().isEmpty())
    {
      InfoDialog.showInfo("Geben Sie einen Dateinamen an.", null, 0);
      return true;
    }

    String lOutputFileName = dialog.getOutputFileTextField().getText().trim();
    File lOutputFile = new File(lOutputFileName);
    if (lOutputFile.exists())
    {
      if (lOutputFile.canWrite())
      {
        InputDialog inputDialog = new InputDialog("Fehler beim PDF-Druck",
            "Eine Datei namens " + lOutputFileName
                + " existiert bereits, wollen Sie sie überschreiben?",
            80);
        if (!inputDialog.askForInput())
        {
          return true;
        }
      } else
      {
        InfoDialog.showInfo(
            "Eine Datei namens " + lOutputFileName
                + " existiert bereits, kann aber nicht überschrieben werden. Bitte prüfen Sie ob Sie Schreibrechte auf die Datei haben, und wählen Sie gegebenenfalls eine andere Datei aus.",
            null, 80);
        return true;
      }
    }
    return false;
  }

  private boolean checkViewer()
  {
    if (dialog.getViewerCheckBox().isSelected()
        && (dialog.getViewerTextField().getText() == null
            || dialog.getViewerTextField().getText().trim().isEmpty()))
    {
      InfoDialog.showInfo("Wählen Sie ein Programm für die Anzeige aus.", null,
          0);
      return true;
    }
    return false;
  }
}
