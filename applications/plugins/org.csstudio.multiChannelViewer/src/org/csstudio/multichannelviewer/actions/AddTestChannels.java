package org.csstudio.multichannelviewer.actions;

import static gov.bnl.channelfinder.api.Property.Builder.property;
import static gov.bnl.channelfinder.api.Tag.Builder.tag;
import static gov.bnl.channelfinder.api.Channel.Builder.channel;

import org.csstudio.utility.channel.nsls2.CSSChannelFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Random;

import gov.bnl.channelfinder.api.Channel;
import gov.bnl.channelfinder.api.ChannelFinderClient;

import org.csstudio.multichannelviewer.ChannelsListView;
import org.csstudio.multichannelviewer.model.CSSChannelGroup;
import org.csstudio.utility.channel.ICSSChannel;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.PlatformUI;

public class AddTestChannels implements IViewActionDelegate {
	private Random generator = new Random(19580427);
	private IViewPart view;
	private Collection<ICSSChannel> channels;

	CSSChannelFactory factory = CSSChannelFactory.getInstance();

	@Override
	public void init(IViewPart view) {
		// TODO Auto-generated method stub
		this.view = view;
	}

	@Override
	public void run(IAction action) {
		channels = new ArrayList<ICSSChannel>();

		for (int i = 0; i < 2000; i++) {
			String channelName = "Test_";
			channelName += getName(i);
			Channel.Builder channel = channel(channelName).owner("shroffk")
					.with(property("Test_PropA", Integer.toString(i)).owner(
							"shroffk"));
			if (i < 1000)
				channel.with(tag("Test_TagA", "shroffk"));
			if ((i >= 500) || (i < 1500))
				channel.with(tag("Test_TagB", "Shroffk"));
			channel.with(property("Test_PropB",
					Integer.toString(generator.nextInt(100))));
			channel.with(property("Test_PropC", "ALL"));
			channels.add(factory.getCSSChannel(channel.build()));
		}
		// Add all the channels;
		try {
			ChannelsListView viewB = (ChannelsListView) view
					.getSite()
					.getPage()
					.findView("org.csstudio.multichannelviewer.ChannelListView");
			if (viewB != null) {
				viewB.setChannelsGroup(new CSSChannelGroup("Test Channels", channels));
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static String getName(int i) {
		if (i < 1000)
			return "first:" + getName500(i);
		else
			return "second:" + getName500(i - 1000);
	}

	private static String getName500(int i) {
		if (i < 500)
			return "a" + getName100(i);
		else
			return "b" + getName100(i - 500);
	}

	private static String getName100(int i) {
		return "<" + Integer.toString(i / 100) + "00>" + getNameID(i % 100);
	}

	private static String getNameID(int i) {
		return ":" + Integer.toString(i / 10) + ":" + Integer.toString(i);
	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		// TODO Auto-generated method stub
	}

}