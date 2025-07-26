package fiji.plugin.trackmate.action;

import fiji.plugin.trackmate.util.cli.CondaCLIConfigurator;

public class InTRACKtiveCLI extends CondaCLIConfigurator
{

	private final PathArgument geffFile;

	public InTRACKtiveCLI()
	{
		this.geffFile = addPathArgument()
				.name( "GEFF file" )
				.help( "Path to the GEFF file where TrackMate data is exported." )
				.argument( "" )
				.visible( false )
				.required( true )
				.get();
	}

	public PathArgument geffFile()
	{
		return geffFile;
	}

	@Override
	protected String getCommand()
	{
		return "intracktive open";
	}

}
