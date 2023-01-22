package ihm;

import javax.swing.JPanel;
import javax.swing.JSplitPane;

import java.awt.Color;
import java.awt.BorderLayout;

import controleur.Controleur;


public class PanelGlobal extends JPanel
{
    private Controleur          ctrl;

    private PanelFonctionGlobal panelFonctionGlobal;
    private PanelGauche         panelGauche;
    private PanelDroite         panelDroite;
    private JSplitPane          panelSpliter;


    public PanelGlobal(Controleur ctrl)
    {
        this.ctrl = ctrl;

        /*--------------------------*/
        /* Créations des composants */
        /*--------------------------*/
        /* Ce Panel */
        this.setLayout(new BorderLayout());

        /* Autres Panels */
        this.panelFonctionGlobal = new PanelFonctionGlobal(this.ctrl);
        this.panelGauche         = new PanelGauche        (this.ctrl);
        this.panelDroite         = new PanelDroite        (this.ctrl);
        this.panelSpliter        = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, panelGauche, panelDroite);


        /*-----------------------*/
        /* Ajouts des composants */
        /*-----------------------*/
        /* Panels */
        this.add(this.panelFonctionGlobal, BorderLayout.NORTH );
        this.add(this.panelSpliter       , BorderLayout.CENTER);


        /*---------------------------*/
        /* Activtions des composants */
        /*---------------------------*/
    }


    /**
     * Permet d'appliquer le thème à chaque élément de l'ihm qui en à besoins
     */
    public void appliquerTheme()
    {
        Color backGeneralColor = this.ctrl.getTheme().get("background");
        Color foreGeneralColor = this.ctrl.getTheme().get("foreground");

        /* Ce Panel */
        this.setBackground(backGeneralColor);
        this.setForeground(foreGeneralColor);
        
        /* Panel Spliter */
        this.panelSpliter.setBackground(backGeneralColor);
        this.panelSpliter.setForeground(foreGeneralColor);

        /* Autres Panels */
        this.panelFonctionGlobal.appliquerTheme();
        this.panelGauche.appliquerTheme();
        this.panelDroite.appliquerTheme();
    }
}