package fiji.plugin.trackmate.action;

import static fiji.plugin.trackmate.gui.Fonts.BIG_FONT;
import static fiji.plugin.trackmate.gui.Fonts.FONT;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import org.apache.commons.io.FileUtils;
import org.scijava.prefs.PrefService;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.gui.GuiUtils;
import fiji.plugin.trackmate.gui.Icons;
import fiji.plugin.trackmate.io.TmXmlReader;
import fiji.plugin.trackmate.io.TrackMateGeffWriter;
import fiji.plugin.trackmate.util.TMUtils;
import fiji.plugin.trackmate.util.cli.CLIUtils;
import fiji.plugin.trackmate.util.cli.ConfigGuiBuilder;

public class InTRACKtiveLauncher
{
	private static final InTRACKtiveCLI cli = new InTRACKtiveCLI();

	public static final ImageIcon ICON;
	static
	{
		ImageIcon icon = null;
		try
		{
			final URL resource = GuiUtils.getResource( "images/CZ-Biohub-SF-RGB-60x60.png", InTRACKtiveLauncher.class );
			icon = new ImageIcon( resource );
		}
		catch ( final Exception e )
		{
			e.printStackTrace();
			System.err.println( "Could not load InTRACKtive icon. Using default icon." );
			icon = Icons.TRACK_ICON_64x64;
		}
		ICON = icon;
	}

	private static final String TITLE = "TrackMate InTRACKtive launcher";

	public static final String DOC_URL = "https://github.com/royerlab/inTRACKtive/blob/main/README.md";

	private static final String CONDA_ENV_PREF_KEY = "LastCondaEnv";

	public static void showDialog( final Frame parent, final Model model, final Logger logger, final boolean is2d )
	{
		final PrefService prefService = TMUtils.getContext().getService( PrefService.class );
		String lastCondaEnv = prefService.get( InTRACKtiveLauncher.class, CONDA_ENV_PREF_KEY, "base" );
		cli.getCommandArg().set( lastCondaEnv );

		final JPanel panel = new JPanel();
		panel.setLayout( new BorderLayout() );

		/*
		 * HEADER
		 */

		final JPanel header = new JPanel();
		header.setBorder( BorderFactory.createEmptyBorder( 5, 5, 5, 5 ) );
		header.setLayout( new BoxLayout( header, BoxLayout.Y_AXIS ) );

		final JLabel lbl = new JLabel( TITLE, ICON, JLabel.RIGHT );
		lbl.setFont( BIG_FONT );
		lbl.setHorizontalAlignment( SwingConstants.CENTER );
		lbl.setAlignmentX( JLabel.CENTER_ALIGNMENT );
		header.add( lbl );
		final JEditorPane infoDisplay = GuiUtils.infoDisplay( "<html>" + "Documentation for this module "
				+ "<a href=\"" + DOC_URL + "\">on the InTRACKtive repo</a>."
				+ "</html>", false );
		infoDisplay.setMaximumSize( new Dimension( 100_000, 40 ) );
		header.add( Box.createVerticalStrut( 5 ) );
		header.add( infoDisplay );
		final JLabel l2 = new JLabel( "Specify in what conda env is InTRACKtive installed:" );
		l2.setFont( FONT );
		l2.setHorizontalAlignment( SwingConstants.CENTER );
		l2.setAlignmentX( JLabel.CENTER_ALIGNMENT );

		header.add( Box.createVerticalStrut( 15 ) );
		header.add( l2 );

		panel.add( header, BorderLayout.NORTH );
		panel.add( ConfigGuiBuilder.build( cli ), BorderLayout.CENTER );

		final int result = JOptionPane.showOptionDialog(
				parent,
				panel,
				TITLE,
				JOptionPane.OK_CANCEL_OPTION,
				JOptionPane.PLAIN_MESSAGE,
				null,
				null,
				null );

		if ( result != JOptionPane.OK_OPTION )
			return;


		final boolean success = run( cli, model, logger, is2d );
		if ( success )
		{
			lastCondaEnv = cli.getCommandArg().getValue();
			prefService.put( InTRACKtiveLauncher.class, CONDA_ENV_PREF_KEY, lastCondaEnv );
		}
	}

	public static boolean run( final InTRACKtiveCLI cli, final Model model, final Logger logger, final boolean is2d )
	{
		try
		{
			// Export to a tmp GEFF file.
			final Path maskTmpFolder = Files.createTempDirectory( "TrackMate-InTRACKtive_" );
//			CLIUtils.recursiveDeleteOnShutdownHook( maskTmpFolder );
			final String exportPath = maskTmpFolder.resolve( "trackmate.zarr" ).toString();
			TrackMateGeffWriter.export( model, exportPath, is2d );

			// Pass the path to the TrackMat GEFF in the zarr to the CLI.
			final Path geffPath = Paths.get( exportPath ).resolve( TrackMateGeffWriter.GEFF_PREFIX );

			/*
			 * Temporary fixes.
			 *
			 * 1. Remove the 'polygons' subfolder, as it is not in the current
			 * GEFF spec and would cause the validation to fail.
			 */
			final Path polygonsFolder = geffPath.resolve( "nodes/props/polygon" );
			if ( Files.exists( polygonsFolder ) )
				FileUtils.deleteDirectory( polygonsFolder.toFile() );

			/*
			 * 2. Remove the 'covariance2d' and 'covariance3d' subfolders, as
			 * they are not scalar and will mess with the inTRACKtive
			 * requirements for extra data to be scalar.
			 */
			final Path covariance2dFolder = geffPath.resolve( "nodes/props/covariance2d" );
			if ( Files.exists( covariance2dFolder ) )
				FileUtils.deleteDirectory( covariance2dFolder.toFile() );
			final Path covariance3dFolder = geffPath.resolve( "nodes/props/covariance3d" );
			if ( Files.exists( covariance3dFolder ) )
				FileUtils.deleteDirectory( covariance3dFolder.toFile() );

			cli.geffFile().set( geffPath.toString() );

			// Run the CLI.
			final String logFile = maskTmpFolder.resolve( "trackmate-intracktive.log" ).toString();
			return CLIUtils.execute( cli, logger, new File( logFile ) );
		}
		catch ( final IOException e )
		{
			logger.error( "Could not create temp folder to export TrackMate to GEFF file:\n" + e.getMessage() );
		}
		return false;
	}

	public static void main( final String[] args )
	{
		final String filename = "../TrackMate/samples/MAX_Merged.xml";
		final TmXmlReader reader = new TmXmlReader( new File( filename ) );
		if ( !reader.isReadingOk() )
		{
			System.err.println( "Error reading TrackMate XML file: " + reader.getErrorMessage() );
			return;
		}

		final Model model = reader.getModel();
		final Logger logger = Logger.DEFAULT_LOGGER;

		showDialog( null, model, logger, true );
		System.out.println( "Done." ); // DEBUG
	}
}
