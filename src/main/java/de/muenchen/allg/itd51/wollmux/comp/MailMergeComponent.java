/*
 * Dateiname: WollMux.java
 * Projekt  : WollMux
 * Funktion : zentraler UNO-Service WollMux 
 * 
 * Copyright (c) 2008-2015 Landeshauptstadt München
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the European Union Public Licence (EUPL),
 * version 1.0 (or any later version).
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * European Union Public Licence for more details.
 *
 * You should have received a copy of the European Union Public Licence
 * along with this program. If not, see
 * http://ec.europa.eu/idabc/en/document/7330
 *
 * Änderungshistorie:
 * Datum      | Wer | Änderungsgrund
 * -------------------------------------------------------------------
 * 14.10.2005 | LUT | Erstellung
 * 09.11.2005 | LUT | + Logfile wird jetzt erweitert (append-modus)
 *                    + verwenden des Konfigurationsparameters SENDER_SOURCE
 *                    + Erster Start des wollmux über wm_configured feststellen.
 * 05.12.2005 | BNK | line.separator statt \n                 |  
 * 06.06.2006 | LUT | + Ablösung der Event-Klasse durch saubere Objektstruktur
 * -------------------------------------------------------------------
 *
 * @author Christoph Lutz (D-III-ITD 5.1)
 * @version 1.0
 * 
 */

package de.muenchen.allg.itd51.wollmux.comp;

import java.util.ArrayList;
import java.util.List;

import com.sun.star.beans.PropertyValue;
import com.sun.star.container.XIndexAccess;
import com.sun.star.container.XIndexContainer;
import com.sun.star.form.FormButtonType;
import com.sun.star.frame.DispatchDescriptor;
import com.sun.star.frame.XDispatch;
import com.sun.star.frame.XDispatchProvider;
import com.sun.star.lang.XServiceInfo;
import com.sun.star.lang.XSingleComponentFactory;
import com.sun.star.lib.uno.helper.Factory;
import com.sun.star.lib.uno.helper.WeakBase;
import com.sun.star.registry.XRegistryKey;
import com.sun.star.ui.XModuleUIConfigurationManagerSupplier;
import com.sun.star.ui.XUIConfigurationManager;
import com.sun.star.uno.XComponentContext;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.afid.UnoProps;
import de.muenchen.allg.itd51.wollmux.WollMuxSingleton;
import de.muenchen.allg.itd51.wollmux.core.util.L;
import de.muenchen.allg.itd51.wollmux.event.Dispatch;
import de.muenchen.allg.itd51.wollmux.event.DispatchProviderAndInterceptor;

/**
 * Diese Klasse stellt den zentralen UNO-Service WollMux dar. Der Service hat
 * folgende Funktionen: Als XDispatchProvider und XDispatch behandelt er alle
 * "wollmux:kommando..." URLs und als XWollMux stellt er die Schnittstelle für
 * externe UNO-Komponenten dar. Der Service wird beim Starten von OpenOffice.org
 * automatisch (mehrfach) instanziiert, wenn OOo einen dispatchprovider für die in
 * der Datei Addons.xcu enthaltenen wollmux:... dispatches besorgen möchte (dies
 * geschieht auch bei unsichtbar geöffneten Dokumenten). Als Folge wird das
 * WollMux-Singleton bei OOo-Start (einmalig) initialisiert.
 */
public class MailMergeComponent extends WeakBase implements XServiceInfo, XDispatchProvider
{

  /**
   * Dieses Feld entält eine Liste aller Services, die dieser UNO-Service
   * implementiert.
   */
  private static final java.lang.String[] SERVICENAMES =
    { "de.muenchen.allg.itd51.wollmux.WollMux" };

  /**
   * Der Konstruktor initialisiert das WollMuxSingleton und startet damit den
   * eigentlichen WollMux. Der Konstuktor wird aufgerufen, bevor OpenOffice.org die
   * Methode executeAsync() aufrufen kann, die bei einem ON_FIRST_VISIBLE_TASK-Event
   * über den Job-Mechanismus ausgeführt wird.
   * 
   * @param context
   */
  public MailMergeComponent(XComponentContext ctx)
  {
    WollMuxSingleton.initialize(ctx);
    
    if (!WollMuxSingleton.getInstance().isMenusCreated())
    {
      createMenuItems();
      WollMuxSingleton.getInstance().setMenusCreated(true);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.sun.star.lang.XServiceInfo#getSupportedServiceNames()
   */
  @Override
  public String[] getSupportedServiceNames()
  {
    return SERVICENAMES;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.sun.star.lang.XServiceInfo#supportsService(java.lang.String)
   */
  @Override
  public boolean supportsService(String sService)
  {
    int len = SERVICENAMES.length;
    for (int i = 0; i < len; i++)
    {
      if (sService.equals(SERVICENAMES[i])) return true;
    }
    return false;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.sun.star.lang.XServiceInfo#getImplementationName()
   */
  @Override
  public String getImplementationName()
  {
    return (MailMergeComponent.class.getName());
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.sun.star.frame.XDispatchProvider#queryDispatch(com.sun.star.util.URL,
   * java.lang.String, int)
   */
  @Override
  public XDispatch queryDispatch( /* IN */com.sun.star.util.URL aURL,
  /* IN */String sTargetFrameName,
  /* IN */int iSearchFlags)
  {
    return DispatchProviderAndInterceptor.globalWollMuxDispatches.queryDispatch(
      aURL, sTargetFrameName, iSearchFlags);
  }

  /*
   * (non-Javadoc)
   * 
   * @seecom.sun.star.frame.XDispatchProvider#queryDispatches(com.sun.star.frame.
   * DispatchDescriptor[])
   */
  @Override
  public XDispatch[] queryDispatches( /* IN */DispatchDescriptor[] seqDescripts)
  {
    return DispatchProviderAndInterceptor.globalWollMuxDispatches.queryDispatches(seqDescripts);
  }

  /**
   * Diese Methode liefert eine Factory zurück, die in der Lage ist den UNO-Service
   * zu erzeugen. Die Methode wird von UNO intern benötigt. Die Methoden
   * __getComponentFactory und __writeRegistryServiceInfo stellen das Herzstück des
   * UNO-Service dar.
   * 
   * @param sImplName
   * @return
   */
  public synchronized static XSingleComponentFactory __getComponentFactory(
      java.lang.String sImplName)
  {
    com.sun.star.lang.XSingleComponentFactory xFactory = null;
    if (sImplName.equals(MailMergeComponent.class.getName()))
      xFactory = Factory.createComponentFactory(MailMergeComponent.class, SERVICENAMES);

    return xFactory;
  }

  /**
   * Diese Methode registriert den UNO-Service. Sie wird z.B. beim unopkg-add im
   * Hintergrund aufgerufen. Die Methoden __getComponentFactory und
   * __writeRegistryServiceInfo stellen das Herzstück des UNO-Service dar.
   * 
   * @param xRegKey
   * @return
   */
  public synchronized static boolean __writeRegistryServiceInfo(XRegistryKey xRegKey)
  {
    try
    {
      return Factory.writeRegistryServiceInfo(MailMergeComponent.class.getName(),
        MailMergeComponent.SERVICENAMES, xRegKey);
    }
    catch (Throwable t)
    {
      // Es ist besser hier alles zu fangen was fliegt und es auf stderr auszugeben.
      // So kann man z.B. mit "unopkg add <paketname>" immer gleich sehen, warum sich
      // die Extension nicht installieren lässt. Fängt man hier nicht, erzeugt
      // "unopkg add" eine unverständliche Fehlerausgabe und man sucht lange nach der
      // Ursache. Wir hatten bei java-Extensions vor allem schon Probleme mit
      // verschiedenen OOo/LO-Versionen, die wir erst finden konnten, als wir die
      // Exception ausgegeben haben. Die Logger-Klasse möchte ich hier für die
      // Ausgabe nicht verwenden weil dies ein Problem während der Installation und
      // nicht während der Laufzeit ist.
      t.printStackTrace();
      return false;
    }
  }
  
  private void createMenuItems()
  {
    // "Extras->Seriendruck (WollMux)" erzeugen:
    List<String> removeButtonsFor = new ArrayList<String>();
    removeButtonsFor.add(Dispatch.DISP_wmSeriendruck);
    MailMergeComponent.createMenuButton(Dispatch.DISP_wmSeriendruck, L.m("Seriendruck (WollMux)"),
      ".uno:ToolsMenu", ".uno:MailMergeWizard", removeButtonsFor);
  }

  /**
   * Erzeugt einen persistenten Menüeintrag mit der KommandoUrl cmdUrl und dem Label
   * label in dem durch mit insertIntoMenuUrl beschriebenen Toplevelmenü des Writers
   * und ordnet ihn direkt oberhalb des bereits bestehenden Menüpunktes mit der URL
   * insertBeforeElementUrl an. Alle Buttons, deren Url in der Liste removeCmdUrls
   * aufgeführt sind werden dabei vorher gelöscht (v.a. sollte cmdUrl aufgeführt
   * sein, damit nicht der selbe Button doppelt erscheint).
   */
  private static void createMenuButton(String cmdUrl, String label,
      String insertIntoMenuUrl, String insertBeforeElementUrl,
      List<String> removeCmdUrls)
  {
    final String settingsUrl = "private:resource/menubar/menubar";
  
    try
    {
      // Menüleiste aus des Moduls com.sun.star.text.TextDocument holen:
      XModuleUIConfigurationManagerSupplier suppl =
        UNO.XModuleUIConfigurationManagerSupplier(UNO.createUNOService("com.sun.star.ui.ModuleUIConfigurationManagerSupplier"));
      XUIConfigurationManager cfgMgr =
        UNO.XUIConfigurationManager(suppl.getUIConfigurationManager("com.sun.star.text.TextDocument"));
      XIndexAccess menubar = UNO.XIndexAccess(cfgMgr.getSettings(settingsUrl, true));
  
      int idx = findElementWithCmdURL(menubar, insertIntoMenuUrl);
      if (idx >= 0)
      {
        UnoProps desc = new UnoProps((PropertyValue[]) menubar.getByIndex(idx));
        // Elemente des .uno:ToolsMenu besorgen:
        XIndexContainer toolsMenu =
          UNO.XIndexContainer(desc.getPropertyValue("ItemDescriptorContainer"));
  
        // Seriendruck-Button löschen, wenn er bereits vorhanden ist.
        for (String rCmdUrl : removeCmdUrls)
        {
          idx = findElementWithCmdURL(toolsMenu, rCmdUrl);
          if (idx >= 0) toolsMenu.removeByIndex(idx);
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
    }
    catch (Exception e)
    {}
  }
  
  /**
   * Liefert den Index des ersten Menüelements aus dem Menü menu zurück, dessen
   * CommandURL mit cmdUrl identisch ist oder -1, falls kein solches Element gefunden
   * wurde.
   * 
   * @return Liefert den Index des ersten Menüelements mit CommandURL cmdUrl oder -1.
   */
  private static int findElementWithCmdURL(XIndexAccess menu, String cmdUrl)
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
    }
    catch (Exception e)
    {}
    return -1;
  }
  
}
