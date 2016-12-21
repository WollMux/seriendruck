/*
 * Dateiname: MailMergeNew.java
 * Projekt  : WollMux
 * Funktion : Die neuen erweiterten Serienbrief-Funktionalitäten
 * 
 * Copyright (c) 2010-2015 Landeshauptstadt München
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
 * 11.10.2007 | BNK | Erstellung
 * 25.05.2010 | ERT | Aufruf von PDFGesamtdruck-Druckfunktion
 * 20.12.2010 | ERT | Bei ungültigem indexSelection.rangeEnd wird der 
 *                    Wert auf den letzten Datensatz gesetzt
 * 08.05.2012 | jub | um beim serienbrief/emailversand die auswahl zwischen odt und pdf
 *                    anhängen anbieten zu können, sendAsEmail() und saveToFile() mit 
 *                    einer flage versehen, die zwischen den beiden formaten 
 *                    unterscheidet.
 * -------------------------------------------------------------------
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 * 
 */
package de.muenchen.allg.itd51.wollmux.dialog.mailmerge;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.WindowConstants;

import com.sun.star.awt.XTopWindow;
import com.sun.star.beans.IllegalTypeException;
import com.sun.star.beans.NotRemoveableException;
import com.sun.star.beans.PropertyAttribute;
import com.sun.star.beans.PropertyExistException;
import com.sun.star.beans.PropertyValue;
import com.sun.star.beans.PropertyVetoException;
import com.sun.star.beans.UnknownPropertyException;
import com.sun.star.beans.XPropertyContainer;
import com.sun.star.beans.XPropertySet;
import com.sun.star.document.XDocumentPropertiesSupplier;
import com.sun.star.frame.XFrame;
import com.sun.star.frame.XStorable;
import com.sun.star.lang.IllegalArgumentException;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.text.XTextDocument;
import com.sun.star.uno.UnoRuntime;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.wollmux.core.dialog.DimAdjust;
import de.muenchen.allg.itd51.wollmux.core.dialog.JPotentiallyOverlongPopupMenuButton;
import de.muenchen.allg.itd51.wollmux.core.document.TextDocumentModel;
import de.muenchen.allg.itd51.wollmux.core.exceptions.UnavailableException;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.core.util.L;
import de.muenchen.allg.itd51.wollmux.core.util.Logger;
import de.muenchen.allg.itd51.wollmux.dialog.Common;
import de.muenchen.allg.itd51.wollmux.dialog.NonNumericKeyConsumer;
import de.muenchen.allg.itd51.wollmux.dialog.trafo.GenderDialog;
import de.muenchen.allg.itd51.wollmux.dialog.trafo.TrafoDialog;
import de.muenchen.allg.itd51.wollmux.dialog.trafo.TrafoDialogFactory;
import de.muenchen.allg.itd51.wollmux.dialog.trafo.TrafoDialogParameters;
import de.muenchen.allg.itd51.wollmux.document.TextDocumentController;
import de.muenchen.allg.itd51.wollmux.event.MailMergeEventHandler;

/**
 * Die neuen erweiterten Serienbrief-Funktionalitäten.
 * 
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class MailMergeNew
{
  public static final String PROP_MAILMERGENEW = "mailMergeNew";
  private static Map<String, MailMergeNew> instances = new ConcurrentHashMap<>();
  /**
   * true gdw wir uns im Vorschau-Modus befinden.
   */
  private boolean previewMode;

  /**
   * Die Nummer des zu previewenden Datensatzes. ACHTUNG! Kann aufgrund von
   * Veränderung der Daten im Hintergrund größer sein als die Anzahl der Datensätze.
   * Darauf muss geachtet werden.
   */
  private int previewDatasetNumber = 1;

  /**
   * Die beim letzten Aufruf von {@link #updatePreviewFields()} aktuelle Anzahl an
   * Datensätzen in {@link #ds}.
   */
  private int previewDatasetNumberMax = Integer.MAX_VALUE;

  /**
   * Das Textfield in dem Benutzer direkt eine Datensatznummer für die Vorschau
   * eingeben können.
   */
  private JTextField previewDatasetNumberTextfield;

  private Collection<JComponent> elementsDisabledWhenNoDatasourceSelected =
    new Vector<JComponent>();

  private Collection<JComponent> elementsDisabledWhenNotInPreviewMode =
    new Vector<JComponent>();

  private Collection<JComponent> elementsDisabledWhenFirstDatasetSelected =
    new Vector<JComponent>();

  private Collection<JComponent> elementsDisabledWhenLastDatasetSelected =
    new Vector<JComponent>();

  /**
   * Enthält alle elementsDisabledWhen... Collections.
   */
  private Vector<Collection<JComponent>> listsOfElementsDisabledUnderCertainCircumstances =
    new Vector<Collection<JComponent>>();

  /**
   * Das Toolbar-Fenster.
   */
  private JFrame myFrame;

  /**
   * Der WindowListener, der an {@link #myFrame} hängt.
   */
  private MyWindowListener windowListener;

  /**
   * Falls nicht null wird dieser Listener aufgerufen nachdem der MailMergeNew
   * geschlossen wurde.
   */
  private ActionListener abortListener = null;

  private MailMergeParams mailMergeParams = new MailMergeParams();

  /**
   * Enthält den Controller, der an das Dokumentfenster dieses Dokuments angekoppelte
   * Fenster überwacht und steuert.
   */
  private CoupledWindowController coupledWindowController = null;

  private TextDocumentController documentController;

  private MailMergeControllerImpl mailMergeController;
  
  

  /**
   * Die zentrale Klasse, die die Serienbrieffunktionalität bereitstellt.
   * 
   * @param documentController
   *          das {@link TextDocumentModel} an dem die Toolbar hängt.
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  public MailMergeNew(TextDocumentController documentController, ActionListener abortListener)
  {
    this.mailMergeController = new MailMergeControllerImpl(documentController);
    this.documentController = documentController;
    this.abortListener = abortListener;
    
    String uuid = UUID.randomUUID().toString();
    instances.put(uuid, this);
    
    XTextDocument doc = documentController.getModel().doc;
    XDocumentPropertiesSupplier propSupplier = UnoRuntime.queryInterface(XDocumentPropertiesSupplier.class, doc);
    XPropertyContainer userDefinedProperties = propSupplier.getDocumentProperties().getUserDefinedProperties();
    XPropertySet props = UNO.XPropertySet(userDefinedProperties);
    if (!props.getPropertySetInfo().hasPropertyByName(PROP_MAILMERGENEW)) {
      try
      {
        userDefinedProperties.addProperty(PROP_MAILMERGENEW, (short) (PropertyAttribute.TRANSIENT), "None");
      } catch (IllegalArgumentException | PropertyExistException | IllegalTypeException e)
      {
        Logger.error(e);
      }
    }
    
    try
    {
      props.setPropertyValue(PROP_MAILMERGENEW, uuid);
    } catch (IllegalArgumentException | UnknownPropertyException | PropertyVetoException | WrappedTargetException e)
    {
      Logger.error(e);
    }
  }
  
  public static MailMergeNew getInstance(String uuid)
  {
    return instances.get(uuid);
  }
  
  public static void disposeInstance(TextDocumentController documentController)
  {
    XTextDocument doc = documentController.getModel().doc;
    XDocumentPropertiesSupplier propSupplier = UnoRuntime.queryInterface(XDocumentPropertiesSupplier.class, doc);
    XPropertyContainer userDefinedProperties = propSupplier.getDocumentProperties().getUserDefinedProperties();
    XPropertySet props = UNO.XPropertySet(userDefinedProperties);
    if (props.getPropertySetInfo().hasPropertyByName(MailMergeNew.PROP_MAILMERGENEW)) {
      try
      {
        String uuid = (String)props.getPropertyValue(MailMergeNew.PROP_MAILMERGENEW);
        MailMergeNew mmn = MailMergeNew.getInstance(uuid);
        mmn.dispose();
        userDefinedProperties.removeProperty(MailMergeNew.PROP_MAILMERGENEW);
      } catch (UnknownPropertyException | WrappedTargetException | NotRemoveableException e)
      {
        Logger.error(e);
      }
    }

  }

  public void run()
  {
    // GUI im Event-Dispatching Thread erzeugen wg. Thread-Safety.
    try
    {
      javax.swing.SwingUtilities.invokeLater(new Runnable()
      {
        @Override
        public void run()
        {
          try
          {
            createGUI();
          }
          catch (Exception x)
          {
            Logger.error(x);
          }
          ;
        }
      });
    }
    catch (Exception x)
    {
      Logger.error(x);
    }
  }

  private void createGUI()
  {
    elementsDisabledWhenNoDatasourceSelected.clear();
    elementsDisabledWhenNotInPreviewMode.clear();
    elementsDisabledWhenFirstDatasetSelected.clear();
    elementsDisabledWhenLastDatasetSelected.clear();
    listsOfElementsDisabledUnderCertainCircumstances.clear();
    listsOfElementsDisabledUnderCertainCircumstances.add(elementsDisabledWhenNoDatasourceSelected);
    listsOfElementsDisabledUnderCertainCircumstances.add(elementsDisabledWhenNotInPreviewMode);
    listsOfElementsDisabledUnderCertainCircumstances.add(elementsDisabledWhenFirstDatasetSelected);
    listsOfElementsDisabledUnderCertainCircumstances.add(elementsDisabledWhenLastDatasetSelected);

    myFrame = new JFrame(L.m("Seriendruck (WollMux)"));
    myFrame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
    windowListener = new MyWindowListener();
    myFrame.addWindowListener(windowListener);

    // WollMux-Icon für den Seriendruck-Frame
    Common.setWollMuxIcon(myFrame);

    Box hbox = Box.createHorizontalBox();
    myFrame.add(hbox);
    JButton button;
    button = new JButton(L.m("Datenquelle"));
    button.addActionListener(new ActionListener()
    {
      @Override
      public void actionPerformed(ActionEvent e)
      {
        mailMergeController.showDatasourceSelectionDialog(myFrame, new Runnable()
        {
          @Override
          public void run()
          {
            updateEnabledDisabledState();
          }
        });
      }
    });
    hbox.add(button);

    button =
      new JPotentiallyOverlongPopupMenuButton(L.m("Serienbrieffeld"),
        new Iterable<Action>()
        {
          @Override
          public Iterator<Action> iterator()
          {
            return getInsertFieldActionList().iterator();
          }
        });
    hbox.add(button);
    elementsDisabledWhenNoDatasourceSelected.add(button);

    button = new JButton(L.m("Spezialfeld"));
    final JButton specialFieldButton = button;
    button.addActionListener(new ActionListener()
    {
      @Override
      public void actionPerformed(ActionEvent e)
      {
        showInsertSpecialFieldPopup(specialFieldButton, 0,
          specialFieldButton.getSize().height);
      }
    });
    hbox.add(button);

    final String VORSCHAU = L.m("   Vorschau   ");
    button = new JButton(VORSCHAU);
    previewMode = false;
    documentController.setFormFieldsPreviewMode(previewMode);

    final JButton previewButton = button;
    button.addActionListener(new ActionListener()
    {
      @Override
      public void actionPerformed(ActionEvent e)
      {
        if (!mailMergeController.hasDatasource()) return;
        if (previewMode)
        {
          documentController.collectNonWollMuxFormFields();
          previewButton.setText(VORSCHAU);
          previewMode = false;
          documentController.setFormFieldsPreviewMode(false);
          updateEnabledDisabledState();
        }
        else
        {
          documentController.collectNonWollMuxFormFields();
          previewButton.setText(L.m("<Feldname>"));
          previewMode = true;
          documentController.setFormFieldsPreviewMode(true);
          updatePreviewFields();
        }
      }
    });
    hbox.add(DimAdjust.fixedPreferredSize(button));
    elementsDisabledWhenNoDatasourceSelected.add(button);

    button = new JButton("|<");
    button.addActionListener(new ActionListener()
    {
      @Override
      public void actionPerformed(ActionEvent e)
      {
        previewDatasetNumber = 1;
        updatePreviewFields();
      }
    });
    hbox.add(button);
    elementsDisabledWhenNotInPreviewMode.add(button);
    elementsDisabledWhenFirstDatasetSelected.add(button);

    button = new JButton("<");
    button.addActionListener(new ActionListener()
    {
      @Override
      public void actionPerformed(ActionEvent e)
      {
        --previewDatasetNumber;
        if (previewDatasetNumber < 1) previewDatasetNumber = 1;
        updatePreviewFields();
      }
    });
    hbox.add(button);
    elementsDisabledWhenNotInPreviewMode.add(button);
    elementsDisabledWhenFirstDatasetSelected.add(button);

    previewDatasetNumberTextfield = new JTextField("1", 3);
    previewDatasetNumberTextfield.addKeyListener(NonNumericKeyConsumer.instance);
    previewDatasetNumberTextfield.addActionListener(new ActionListener()
    {
      @Override
      public void actionPerformed(ActionEvent e)
      {
        String tfValue = previewDatasetNumberTextfield.getText();
        try
        {
          int newValue = Integer.parseInt(tfValue);
          previewDatasetNumber = newValue;
        }
        catch (Exception x)
        {
          previewDatasetNumberTextfield.setText("" + previewDatasetNumber);
        }
        updatePreviewFields();
      }
    });
    previewDatasetNumberTextfield.setMaximumSize(new Dimension(Integer.MAX_VALUE,
      button.getPreferredSize().height));
    hbox.add(previewDatasetNumberTextfield);
    elementsDisabledWhenNotInPreviewMode.add(previewDatasetNumberTextfield);

    button = new JButton(">");
    button.addActionListener(new ActionListener()
    {
      @Override
      public void actionPerformed(ActionEvent e)
      {
        ++previewDatasetNumber;
        updatePreviewFields();
      }
    });
    hbox.add(button);
    elementsDisabledWhenNotInPreviewMode.add(button);
    elementsDisabledWhenLastDatasetSelected.add(button);

    button = new JButton(">|");
    button.addActionListener(new ActionListener()
    {
      @Override
      public void actionPerformed(ActionEvent e)
      {
        previewDatasetNumber = Integer.MAX_VALUE;
        updatePreviewFields();
      }
    });
    hbox.add(button);
    elementsDisabledWhenNotInPreviewMode.add(button);
    elementsDisabledWhenLastDatasetSelected.add(button);

    button = new JButton(L.m("Drucken"));
    button.addActionListener(new ActionListener()
    {
      @Override
      public void actionPerformed(ActionEvent e)
      {
        if (mailMergeController.hasDatasource())
          mailMergeParams.showDoMailmergeDialog(myFrame, mailMergeController);
      }
    });
    hbox.add(button);
    elementsDisabledWhenNoDatasourceSelected.add(button);

    final JPopupMenu tabelleMenu = new JPopupMenu();
    JMenuItem item = new JMenuItem(L.m("Tabelle bearbeiten"));
    item.addActionListener(new ActionListener()
    {
      @Override
      public void actionPerformed(ActionEvent e)
      {
        mailMergeController.bringDatasourceToFront();
      }
    });
    tabelleMenu.add(item);

    final JMenuItem addColumnsMenuItem =
      new JMenuItem(L.m("Tabellenspalten ergänzen"));
    addColumnsMenuItem.addActionListener(new ActionListener()
    {
      @Override
      public void actionPerformed(ActionEvent e)
      {
        mailMergeController.showAddMissingColumnsDialog(myFrame);
      }
    });
    tabelleMenu.add(addColumnsMenuItem);

    final JMenuItem adjustFieldsMenuItem =
      new JMenuItem(L.m("Alle Felder anpassen"));
    adjustFieldsMenuItem.addActionListener(new ActionListener()
    {
      @Override
      public void actionPerformed(ActionEvent e)
      {
        mailMergeController.showAdjustFieldsDialog(myFrame);
      }
    });
    tabelleMenu.add(adjustFieldsMenuItem);

    button = new JButton(L.m("Tabelle"));
    final JButton tabelleButton = button;
    button.addActionListener(new ActionListener()
    {
      @Override
      public void actionPerformed(ActionEvent e)
      {
        // Ausgrauen der Anpassen-Knöpfe, wenn alle Felder mit den
        // entsprechenden Datenquellenfeldern zugeordnet werden können.
        // Tabellenspalten ergänzen wird außerdem ausgegraut, wenn die Datenquelle
        // dies nicht unterstützt
        boolean hasUnmappedFields =
            documentController.getModel().getReferencedFieldIDsThatAreNotInSchema(new HashSet<String>(
            mailMergeController.getColumnNames())).length > 0;
        adjustFieldsMenuItem.setEnabled(hasUnmappedFields);
        addColumnsMenuItem.setEnabled(hasUnmappedFields && mailMergeController.isDatasourceSupportingAddColumns());

        tabelleMenu.show(tabelleButton, 0, tabelleButton.getSize().height);
      }
    });
    hbox.add(button);
    elementsDisabledWhenNoDatasourceSelected.add(button);

    updateEnabledDisabledState();
    myFrame.setAlwaysOnTop(true);
    myFrame.pack();
    int frameWidth = myFrame.getWidth();
    int frameHeight = myFrame.getHeight();
    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    int x = screenSize.width / 2 - frameWidth / 2;
    int y = frameHeight * 3;// screenSize.height/2 - frameHeight/2;
    myFrame.setLocation(x, y);
    myFrame.setResizable(false);
    addCoupledWindow(myFrame);
    myFrame.setVisible(true);

    if (!mailMergeController.hasDatasource())
      mailMergeController.showDatasourceSelectionDialog(myFrame, new Runnable()
      {
        @Override
        public void run()
        {
          updateEnabledDisabledState();
        }
      });
  }

  /**
   * Geht alle Komponenten durch, die unter bestimmten Bedingungen ausgegraut werden
   * müssen und setzt ihren Status korrekt.
   * 
   * @author Matthias Benkmann (D-III-ITD-D101)
   * 
   */
  private void updateEnabledDisabledState()
  {
    // Zuerst alles enablen.
    for (Collection<JComponent> list : listsOfElementsDisabledUnderCertainCircumstances)
    {
      for (JComponent compo : list)
      {
        compo.setEnabled(true);
      }
    }

    if (!mailMergeController.hasDatasource())
      for (JComponent compo : elementsDisabledWhenNoDatasourceSelected)
        compo.setEnabled(false);

    if (previewDatasetNumber <= 1)
      for (JComponent compo : elementsDisabledWhenFirstDatasetSelected)
        compo.setEnabled(false);

    if (previewDatasetNumber >= previewDatasetNumberMax)
      for (JComponent compo : elementsDisabledWhenLastDatasetSelected)
        compo.setEnabled(false);

    if (!previewMode) for (JComponent compo : elementsDisabledWhenNotInPreviewMode)
      compo.setEnabled(false);
  }

  /**
   * Passt {@link #previewDatasetNumber} an, falls sie zu groß oder zu klein ist,
   * setzt {@link #previewDatasetNumberMax} und setzt dann falls {@link #previewMode}
   * == true alle Feldwerte auf die Werte des entsprechenden Datensatzes. Ruft
   * außerdem {@link #updateEnabledDisabledState()} auf.
   * 
   * @author Matthias Benkmann (D-III-ITD D.10)
   * 
   *         TESTED
   */
  private void updatePreviewFields()
  {
    if (!mailMergeController.hasDatasource()) return;

    int count = mailMergeController.getNumberOfDatasets();
    previewDatasetNumberMax = count;

    if (previewDatasetNumber > count) previewDatasetNumber = count;
    if (previewDatasetNumber <= 0) previewDatasetNumber = 1;

    String previewDatasetNumberStr = "" + previewDatasetNumber;
    previewDatasetNumberTextfield.setText(previewDatasetNumberStr);

    updateEnabledDisabledState();

    if (!previewMode) return;

    List<String> schema = mailMergeController.getColumnNames();
    List<String> data = mailMergeController.getValuesForDataset(previewDatasetNumber);

    if (schema.size() != data.size())
    {
      Logger.error(L.m("Daten haben sich zwischen dem Auslesen von Schema und Werten verändert"));
      return;
    }

    Iterator<String> dataIter = data.iterator();
    for (String column : schema)
    {
      MailMergeEventHandler.getInstance().handleSetFormValue(documentController.getModel().doc, column, dataIter.next(), null);
    }
    MailMergeEventHandler.getInstance().handleSetFormValue(documentController.getModel().doc,
      MailMergeParams.TAG_DATENSATZNUMMER, previewDatasetNumberStr, null);
    MailMergeEventHandler.getInstance().handleSetFormValue(documentController.getModel().doc,
      MailMergeParams.TAG_SERIENBRIEFNUMMER, previewDatasetNumberStr, null);
  }

  /**
   * Schliesst den MailMergeNew und alle zugehörigen Fenster.
   * 
   * @author Christoph Lutz (D-III-ITD 5.1)
   */
  public void dispose()
  {
    try
    {
      javax.swing.SwingUtilities.invokeLater(new Runnable()
      {
        @Override
        public void run()
        {
          try
          {
            abort();
          }
          catch (Exception x)
          {
            Logger.error(x);
          }
        }
      });
    }
    catch (Exception x)
    {}
  }

  /**
   * Erzeugt eine Liste mit {@link javax.swing.Action}s für alle Namen aus
   * {@link #ds},getColumnNames(), die ein entsprechendes Seriendruckfeld einfügen.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private List<Action> getInsertFieldActionList()
  {
    List<Action> actions = new Vector<Action>();
    List<String> columnNames = mailMergeController.getColumnNames();

    Collections.sort(columnNames);

    Iterator<String> iter = columnNames.iterator();
    while (iter.hasNext())
    {
      final String name = iter.next();
      Action button = new AbstractAction(name)
      {
        private static final long serialVersionUID = 0; // Eclipse-Warnung totmachen

        @Override
        public void actionPerformed(ActionEvent e)
        {
          documentController.insertMailMergeFieldAtCursorPosition(name);
        }
      };
      actions.add(button);
    }

    return actions;
  }

  /**
   * Erzeugt ein JPopupMenu, das Einträge für das Einfügen von Spezialfeldern enthält
   * und zeigt es an neben invoker an der relativen Position x,y.
   * 
   * @param invoker
   *          zu welcher Komponente gehört das Popup
   * @param x
   *          Koordinate des Popups im Koordinatenraum von invoker.
   * @param y
   *          Koordinate des Popups im Koordinatenraum von invoker.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void showInsertSpecialFieldPopup(JComponent invoker, int x, int y)
  {
    boolean dsHasFields = mailMergeController.getColumnNames().size() > 0;
    final TrafoDialog editFieldDialog = getTrafoDialogForCurrentSelection();

    JPopupMenu menu = new JPopupMenu();

    JMenuItem button;

    final String genderButtonName = L.m("Gender");
    button = new JMenuItem(genderButtonName);
    button.setEnabled(dsHasFields);
    button.addActionListener(new ActionListener()
    {
      @Override
      public void actionPerformed(ActionEvent e)
      {
        // ConfigThingy für leere Gender-Funktion zusammenbauen.
        ConfigThingy genderConf =
          GenderDialog.generateGenderTrafoConf(mailMergeController.getColumnNames().get(0), "", "",
            "");
        insertFieldFromTrafoDialog(mailMergeController.getColumnNames(), genderButtonName, genderConf);
      }
    });
    menu.add(button);

    final String iteButtonName = L.m("Wenn...Dann...Sonst...");
    button = new JMenuItem(iteButtonName);
    button.setEnabled(dsHasFields);
    button.addActionListener(new ActionListener()
    {
      @Override
      public void actionPerformed(ActionEvent e)
      {
        // ConfigThingy für leere WennDannSonst-Funktion zusammenbauen. Aufbau:
        // IF(STRCMP(VALUE '<firstField>', '') THEN('') ELSE(''))
        ConfigThingy ifConf = new ConfigThingy("IF");
        ConfigThingy strCmpConf = ifConf.add("STRCMP");
        strCmpConf.add("VALUE").add(mailMergeController.getColumnNames().get(0));
        strCmpConf.add("");
        ifConf.add("THEN").add("");
        ifConf.add("ELSE").add("");
        insertFieldFromTrafoDialog(mailMergeController.getColumnNames(), iteButtonName, ifConf);
      }
    });
    menu.add(button);

    button = new JMenuItem(L.m("Datensatznummer"));
    button.addActionListener(new ActionListener()
    {
      @Override
      public void actionPerformed(ActionEvent e)
      {
        documentController.insertMailMergeFieldAtCursorPosition(MailMergeParams.TAG_DATENSATZNUMMER);
      }
    });
    menu.add(button);

    button = new JMenuItem(L.m("Serienbriefnummer"));
    button.addActionListener(new ActionListener()
    {
      @Override
      public void actionPerformed(ActionEvent e)
      {
        documentController.insertMailMergeFieldAtCursorPosition(MailMergeParams.TAG_SERIENBRIEFNUMMER);
      }
    });
    menu.add(button);

    button = new JMenuItem(L.m("Nächster Datensatz"));
    button.addActionListener(new ActionListener()
    {
      @Override
      public void actionPerformed(ActionEvent e)
      {
        documentController.insertNextDatasetFieldAtCursorPosition();
      }
    });
    menu.add(button);

    button = new JMenuItem(L.m("Feld bearbeiten..."));
    button.setEnabled(editFieldDialog != null);
    button.addActionListener(new ActionListener()
    {
      @Override
      public void actionPerformed(ActionEvent e)
      {
        editFieldDialog.show(L.m("Spezialfeld bearbeiten"), myFrame);
      }

    });
    menu.add(button);

    menu.show(invoker, x, y);
  }

  /**
   * Öffnet den Dialog zum Einfügen eines Spezialfeldes, das über die Funktion
   * trafoConf beschrieben ist, erzeugt daraus ein transformiertes Feld und fügt
   * dieses Feld in das Dokument mod ein; Es erwartet darüber hinaus den Namen des
   * Buttons buttonName, aus dem das Label des Dialogs, und später der Mouse-Over
   * hint erzeugt wird und die Liste der aktuellen Felder, die evtl. im Dialog zur
   * Verfügung stehen sollen.
   * 
   * @param fieldNames
   *          Eine Liste der Feldnamen, die der Dialog anzeigt, falls er Buttons zum
   *          Einfügen von Serienbrieffeldern bereitstellt.
   * @param buttonName
   *          Der Name des Buttons, aus dem die Titelzeile des Dialogs und der
   *          Mouse-Over Hint des neu erzeugten Formularfeldes generiert wird.
   * @param trafoConf
   *          ConfigThingy, das die Funktion und damit den aufzurufenden Dialog
   *          spezifiziert. Der von den Dialogen benötigte äußere Knoten
   *          "Func(...trafoConf...) wird dabei von dieser Methode erzeugt, so dass
   *          trafoConf nur die eigentliche Funktion darstellen muss.
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  protected void insertFieldFromTrafoDialog(List<String> fieldNames,
      final String buttonName, ConfigThingy trafoConf)
  {
    TrafoDialogParameters params = new TrafoDialogParameters();
    params.conf = new ConfigThingy("Func");
    params.conf.addChild(trafoConf);
    params.isValid = true;
    params.fieldNames = fieldNames;
    params.closeAction = new ActionListener()
    {
      @Override
      public void actionPerformed(ActionEvent e)
      {
        TrafoDialog dialog = (TrafoDialog) e.getSource();
        TrafoDialogParameters status = dialog.getExitStatus();
        if (status.isValid)
        {
          try
          {
            documentController.replaceSelectionWithTrafoField(status.conf, buttonName);
          }
          catch (Exception x)
          {
            Logger.error(x);
          }
        }
      }
    };

    try
    {
      TrafoDialogFactory.createDialog(params).show(
        L.m("Spezialfeld %1 einfügen", buttonName), myFrame);
    }
    catch (UnavailableException e)
    {
      Logger.error(L.m("Das darf nicht passieren!"));
    }
  }

  /**
   * Prüft, ob sich in der akutellen Selektion ein transformiertes Feld befindet und
   * liefert ein mit Hilfe der TrafoDialogFactory erzeugtes zugehöriges
   * TrafoDialog-Objekt zurück, oder null, wenn keine transformierte Funktion
   * selektiert ist oder für die Trafo kein Dialog existiert.
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  private TrafoDialog getTrafoDialogForCurrentSelection()
  {
    ConfigThingy trafoConf = documentController.getModel().getFormFieldTrafoFromSelection();
    if (trafoConf == null) return null;

    final String trafoName = trafoConf.getName();

    TrafoDialogParameters params = new TrafoDialogParameters();
    params.conf = trafoConf;
    params.isValid = true;
    params.fieldNames = mailMergeController.getColumnNames();
    params.closeAction = new ActionListener()
    {
      @Override
      public void actionPerformed(ActionEvent e)
      {
        TrafoDialog dialog = (TrafoDialog) e.getSource();
        TrafoDialogParameters status = dialog.getExitStatus();
        if (status.isValid)
        {
          try
          {
            documentController.setTrafo(trafoName, status.conf);
          }
          catch (Exception x)
          {
            Logger.error(x);
          }
        }
      }
    };

    try
    {
      return TrafoDialogFactory.createDialog(params);
    }
    catch (UnavailableException e)
    {
      return null;
    }
  }

  /**
   * grobe Plausiprüfung, ob E-Mailadresse gültig ist.
   * 
   * @author Christoph Lutz (D-III-ITD-D101)
   */
  static boolean isMailAddress(String mail)
  {
    return mail != null && mail.length() > 0 && mail.matches("[^ ]+@[^ ]+");
  }

  /**
   * Speichert doc unter dem in outFile angegebenen Dateipfad und schließt dann doc.
   * 
   * @author Matthias Benkmann (D-III-ITD-D101)
   * 
   *         TESTED
   */
  static void saveOutputFile(File outFile, XTextDocument doc)
  {
    try
    {
      String unparsedUrl = outFile.toURI().toURL().toString();

      XStorable store = UNO.XStorable(doc);
      PropertyValue[] options;

      /*
       * For more options see:
       * 
       * http://wiki.services.openoffice.org/wiki/API/Tutorials/PDF_export
       */
      if (unparsedUrl.endsWith(".pdf"))
      {
        options = new PropertyValue[1];

        options[0] = new PropertyValue();
        options[0].Name = "FilterName";
        options[0].Value = "writer_pdf_Export";
      }
      else if (unparsedUrl.endsWith(".doc"))
      {
        options = new PropertyValue[1];

        options[0] = new PropertyValue();
        options[0].Name = "FilterName";
        options[0].Value = "MS Word 97";
      }
      else
      {
        if (!unparsedUrl.endsWith(".odt")) unparsedUrl = unparsedUrl + ".odt";

        options = new PropertyValue[0];
      }

      com.sun.star.util.URL url = UNO.getParsedUNOUrl(unparsedUrl);

      /*
       * storeTOurl() has to be used instead of storeASurl() for PDF export
       */
      store.storeToURL(url.Complete, options);
    }
    catch (Exception x)
    {
      Logger.error(x);
    }
  }

  /**
   * Holt sich Element key aus dataset, sorgt dafür, dass der Wert digit-stellig wird
   * und speichert diesen Wert wieder in dataset ab.
   * 
   * @author Christoph Lutz (D-III-ITD-D101)
   */
  static void fillWithLeading0(HashMap<String, String> dataset, String key,
      int digits)
  {
    String value = dataset.get(key);
    if (value == null) value = "";
    while (value.length() < digits)
      value = "0" + value;
    dataset.put(key, value);
  }

  /**
   * Ersetzt alle möglicherweise bösen Zeichen im Dateinamen name durch eine
   * Unterstrich.
   * 
   * @author Christoph Lutz (D-III-ITD-D101)
   */
  static String simplifyFilename(String name)
  {
    return name.replaceAll("[^\\p{javaLetterOrDigit},.()=+_-]", "_");
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * de.muenchen.allg.itd51.wollmux.dialog.mailmerge.MailMergeParams.MailMergeController
   * #getTextDocument()
   */
  private class MyWindowListener extends WindowAdapter
  {
    @Override
    public void windowClosing(WindowEvent e)
    {
      abort();
    }
  }

  private void abort()
  {
    if (myFrame != null)
    {
      removeCoupledWindow(myFrame);
      /*
       * Wegen folgendem Java Bug (WONTFIX)
       * http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4259304 sind die folgenden
       * 3 Zeilen nötig, damit der MailMerge gc'ed werden kann. Die Befehle sorgen
       * dafür, dass kein globales Objekt (wie z.B. der Keyboard-Fokus-Manager)
       * indirekt über den JFrame den MailMerge kennt.
       */
      myFrame.removeWindowListener(windowListener);
      myFrame.getContentPane().remove(0);
      myFrame.setJMenuBar(null);
  
      myFrame.dispose();
      myFrame = null;
  
      mailMergeController.close();
  
      if (abortListener != null)
      {
        abortListener.actionPerformed(new ActionEvent(this, 0, ""));
      }
    }
  }

  /**
   * Koppelt das AWT-Window window an das Fenster dieses Textdokuments an. Die
   * Methode muss aufgerufen werden, solange das Fenster window unsichtbar und nicht
   * aktiv ist (also z.B. vor dem Aufruf von window.setVisible(true)).
   * 
   * @param window
   *          das Fenster, das an das Hauptfenster angekoppelt werden soll.
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  private synchronized void addCoupledWindow(Window window)
  {
    if (window == null) return;
    if (coupledWindowController == null)
    {
      coupledWindowController = new CoupledWindowController();
      XFrame f = documentController.getFrameController().getFrame();
      XTopWindow w = null;
      if (f != null) w = UNO.XTopWindow(f.getContainerWindow());
      if (w != null) coupledWindowController.setTopWindow(w);
    }
  
    coupledWindowController.addCoupledWindow(window);
  }

  /**
   * Löst die Bindung eines angekoppelten Fensters window an das Dokumentfenster.
   * 
   * @param window
   *          das Fenster, dessen Bindung zum Hauptfenster gelöst werden soll. Ist
   *          das Fenster nicht angekoppelt, dann passiert nichts.
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  private synchronized void removeCoupledWindow(Window window)
  {
    if (window == null || coupledWindowController == null) return;
  
    coupledWindowController.removeCoupledWindow(window);
  
    if (!coupledWindowController.hasCoupledWindows())
    {
      // deregistriert den windowListener.
      XFrame f = documentController.getFrameController().getFrame();
      XTopWindow w = null;
      if (f != null) w = UNO.XTopWindow(f.getContainerWindow());
      if (w != null) coupledWindowController.unsetTopWindow(w);
      coupledWindowController = null;
    }
  }
}
