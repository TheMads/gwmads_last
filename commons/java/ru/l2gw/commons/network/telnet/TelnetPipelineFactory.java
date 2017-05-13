package ru.l2gw.commons.network.telnet;

import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.handler.codec.frame.DelimiterBasedFrameDecoder;
import org.jboss.netty.handler.codec.frame.Delimiters;
import org.jboss.netty.handler.codec.string.StringDecoder;
import org.jboss.netty.handler.codec.string.StringEncoder;

import java.nio.charset.Charset;

import static org.jboss.netty.channel.Channels.pipeline;

/**
 * @author: rage
 * @date: 03.03.12 16:51
 */
public class TelnetPipelineFactory implements ChannelPipelineFactory
{
	private final ChannelHandler handler;
	private final String encoding;

	public TelnetPipelineFactory(ChannelHandler handler, String encoding)
	{
		this.handler = handler;
		this.encoding = encoding;
	}

	@Override
	public ChannelPipeline getPipeline() throws Exception
	{
		// Create a default pipeline implementation.
		ChannelPipeline pipeline = pipeline();

		// Add the text line codec combination first,
		pipeline.addLast("framer", new DelimiterBasedFrameDecoder(8192, Delimiters.lineDelimiter()));
		pipeline.addLast("decoder", new StringDecoder(Charset.forName(encoding)));
		pipeline.addLast("encoder", new StringEncoder(Charset.forName(encoding)));

		// and then business logic.
		pipeline.addLast("handler", handler);

		return pipeline;
	}
}
