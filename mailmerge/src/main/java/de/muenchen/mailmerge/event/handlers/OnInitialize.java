package de.muenchen.mailmerge.event.handlers;

import java.util.ArrayList;
import java.util.List;

import com.sun.star.beans.PropertyValue;
import com.sun.star.container.XIndexAccess;
import com.sun.star.container.XIndexContainer;
import com.sun.star.form.FormButtonType;
import com.sun.star.ui.XModuleUIConfigurationManagerSupplier;
import com.sun.star.ui.XUIConfigurationManager;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.afid.UnoProps;
import de.muenchen.allg.itd51.wollmux.core.util.L;
import de.muenchen.mailmerge.event.Dispatch;

/**
 * Dieses Event wird als erstes WollMuxEvent bei der Initialisierung des WollMux
 * im WollMuxSingleton erzeugt und übernimmt alle benutzersichtbaren
 * (interaktiven) Initialisierungen.
 *
 * @author christoph.lutz TESTED
 */
public class OnInitialize extends BasicEvent
{
  @Override
  protected void doit()
  {
    createMenuItems();
  }

  private void createMenuItems()
  {
    // "Extras->Seriendruck (WollMux)" erzeugen:
    List<String> removeButtonsFor = new ArrayList<>();
    removeButtonsFor.add(Dispatch.DISP_wmSeriendruck);
    removeButtonsFor.add(Dispatch.DISP_wmAbout);
    createMenuButton(Dispatch.DISP_wmSeriendruck, L.m("Seriendruck (WollMux)"), ".uno:ToolsMenu",
        ".uno:MailMergeWizard", removeButtonsFor);
    createMenuButton(Dispatch.DISP_wmAbout, L.m("Info über Seriendruck (WollMux)"), ".uno:HelpMenu",
        ".uno:About", removeButtonsFor);
  }

  /**
   * Erzeugt einen persistenten Menüeintrag mit der KommandoUrl cmdUrl und dem
   * Label label in dem durch mit insertIntoMenuUrl beschriebenen Toplevelmenü
   * des Writers und ordnet ihn direkt oberhalb des bereits bestehenden
   * Menüpunktes mit der URL insertBeforeElementUrl an. Alle Buttons, deren Url
   * in der Liste removeCmdUrls aufgeführt sind werden dabei vorher gelöscht
   * (v.a. sollte cmdUrl aufgeführt sein, damit nicht der selbe Button doppelt
   * erscheint).
   */
  private void createMenuButton(String cmdUrl, String label,
      String insertIntoMenuUrl, String insertBeforeElementUrl,
      List<String> removeCmdUrls)
  {
    final String settingsUrl = "private:resource/menubar/menubar";

    try
    {
      // Menüleiste aus des Moduls com.sun.star.text.TextDocument holen:
      XModuleUIConfigurationManagerSupplier suppl = UNO
          .XModuleUIConfigurationManagerSupplier(UNO.createUNOService(
              "com.sun.star.ui.ModuleUIConfigurationManagerSupplier"));
      XUIConfigurationManager cfgMgr = UNO.XUIConfigurationManager(
          suppl.getUIConfigurationManager("com.sun.star.text.TextDocument"));
      XIndexAccess menubar = UNO
          .XIndexAccess(cfgMgr.getSettings(settingsUrl, true));

      int idx = findElementWithCmdURL(menubar, insertIntoMenuUrl);
      if (idx >= 0)
      {
        UnoProps desc = new UnoProps((PropertyValue[]) menubar.getByIndex(idx));
        // Elemente des .uno:ToolsMenu besorgen:
        XIndexContainer toolsMenu = UNO
            .XIndexContainer(desc.getPropertyValue("ItemDescriptorContainer"));

        // Seriendruck-Button löschen, wenn er bereits vorhanden ist.
        for (String rCmdUrl : removeCmdUrls)
        {
          idx = findElementWithCmdURL(toolsMenu, rCmdUrl);
          if (idx >= 0)
            toolsMenu.removeByIndex(idx);
        }

        // SeriendruckAssistent suchen
        idx = findElementWithCmdURL(toolsMenu, insertBeforeElementUrl);
        if (idx >= 0)
        {
          UnoProps newDesc = new UnoProps();
          newDesc.setPropertyValue("CommandURL", cmdUrl);
          newDesc.setPropertyValue("Type", FormButtonType.PUSH);
          newDesc.setPropertyValue("Label", label);
          toolsMenu.insertByIndex(idx, newDesc.getProps());
          cfgMgr.replaceSettings(settingsUrl, menubar);
          UNO.XUIConfigurationPersistence(cfgMgr).store();
        }
      }
    } catch (Exception e)
    {
    }
  }

  /**
   * Liefert den Index des ersten Menüelements aus dem Menü menu zurück, dessen
   * CommandURL mit cmdUrl identisch ist oder -1, falls kein solches Element
   * gefunden wurde.
   *
   * @return Liefert den Index des ersten Menüelements mit CommandURL cmdUrl
   *         oder -1.
   */
  private int findElementWithCmdURL(XIndexAccess menu, String cmdUrl)
  {
    try
    {
      for (int i = 0; i < menu.getCount(); ++i)
      {
        PropertyValue[] desc = (PropertyValue[]) menu.getByIndex(i);
        for (int j = 0; j < desc.length; j++)
        {
          if ("CommandURL".equals(desc[j].Name) && cmdUrl.equals(desc[j].Value))
            return i;
        }
      }
    } catch (Exception e)
    {
    }
    return -1;
  }

}