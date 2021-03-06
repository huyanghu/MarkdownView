package br.tiagohm.markdownview;

import android.content.Context;
import android.content.res.TypedArray;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.FrameLayout;

import com.orhanobut.logger.Logger;
import com.vladsch.flexmark.ast.Image;
import com.vladsch.flexmark.ast.Node;
import com.vladsch.flexmark.ast.util.TextCollectingVisitor;
import com.vladsch.flexmark.ext.abbreviation.AbbreviationExtension;
import com.vladsch.flexmark.ext.autolink.AutolinkExtension;
import com.vladsch.flexmark.ext.footnotes.FootnoteExtension;
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughSubscriptExtension;
import com.vladsch.flexmark.ext.gfm.tasklist.TaskListExtension;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.html.CustomNodeRenderer;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.html.HtmlWriter;
import com.vladsch.flexmark.html.renderer.LinkType;
import com.vladsch.flexmark.html.renderer.NodeRenderer;
import com.vladsch.flexmark.html.renderer.NodeRendererContext;
import com.vladsch.flexmark.html.renderer.NodeRendererFactory;
import com.vladsch.flexmark.html.renderer.NodeRenderingHandler;
import com.vladsch.flexmark.html.renderer.ResolvedLink;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.superscript.SuperscriptExtension;
import com.vladsch.flexmark.util.html.Escaping;
import com.vladsch.flexmark.util.options.DataHolder;
import com.vladsch.flexmark.util.options.MutableDataSet;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import br.tiagohm.markdownview.ext.kbd.KeystrokeExtension;
import br.tiagohm.markdownview.ext.mark.MarkExtension;
import br.tiagohm.markdownview.ext.mathjax.MathJaxExtension;

public class MarkdownView extends FrameLayout
{
    private static final DataHolder OPTIONS = new MutableDataSet()
            .set(FootnoteExtension.FOOTNOTE_REF_PREFIX, "[")
            .set(FootnoteExtension.FOOTNOTE_REF_SUFFIX, "]")
            //.set(FootnoteExtension.FOOTNOTE_BACK_REF_STRING, "&#8593")
            ;

    private static final Parser PARSER = Parser.builder(OPTIONS)
            .extensions(Arrays.asList(TablesExtension.create(),
                    TaskListExtension.create(),
                    AbbreviationExtension.create(),
                    AutolinkExtension.create(),
                    MarkExtension.create(),
                    StrikethroughSubscriptExtension.create(),
                    SuperscriptExtension.create(),
                    KeystrokeExtension.create(),
                    MathJaxExtension.create(),
                    FootnoteExtension.create()))
            .build();
    private static final HtmlRenderer.Builder RENDERER_BUILDER = HtmlRenderer.builder(OPTIONS)
            .escapeHtml(true)
            .nodeRendererFactory(new NodeRendererFactoryImpl())
            .extensions(Arrays.asList(TablesExtension.create(),
                    TaskListExtension.create(),
                    AbbreviationExtension.create(),
                    AutolinkExtension.create(),
                    MarkExtension.create(),
                    StrikethroughSubscriptExtension.create(),
                    SuperscriptExtension.create(),
                    KeystrokeExtension.create(),
                    MathJaxExtension.create(),
                    FootnoteExtension.create()));
    private WebView mWebView;

    public MarkdownView(Context context)
    {
        this(context, null);
    }

    public MarkdownView(Context context, AttributeSet attrs)
    {
        this(context, attrs, 0);
    }

    public MarkdownView(Context context, AttributeSet attrs, int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);

        mWebView = new WebView(context, null, 0);
        mWebView.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        try
        {
            mWebView.getSettings().setJavaScriptEnabled(true);
            mWebView.getSettings().setLoadsImagesAutomatically(true);
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }

        try
        {
            TypedArray attr = getContext().obtainStyledAttributes(attrs, R.styleable.MarkdownView);
            setEscapeHtml(attr.getBoolean(R.styleable.MarkdownView_escapeHtml, true));
            attr.recycle();
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }

        addView(mWebView);
    }

    public MarkdownView setEscapeHtml(boolean flag)
    {
        RENDERER_BUILDER.escapeHtml(flag);
        return this;
    }

    public void loadMarkdown(String text, String cssPath)
    {
        Node node = PARSER.parse(text);
        HtmlRenderer renderer = RENDERER_BUILDER.build();
        String html = renderer.render(node);

        StringBuilder sb = new StringBuilder();
        html = sb.append("<html>")
                .append("<head>")
                .append("<link rel=\"stylesheet\" href=\"")
                .append(cssPath == null ? "" : cssPath)
                .append("\" />")
                .append("</head>")
                .append("<body class=\"markdown-body\">")
                .append(html)
                .append("<span id='tooltip'></span>")
                .append("<script type='text/javascript' src='file:///android_asset/js/jquery-3.1.1.min.js'></script>")
                .append("<script type='text/javascript' src='file:///android_asset/js/markdownview.js'></script>")
                .append("<script type=\"text/x-mathjax-config\"> MathJax.Hub.Config({showProcessingMessages: false, showMathMenu: false, tex2jax: {inlineMath: [['$','$']]}});</script>")
                .append("<script type=\"text/javascript\" src=\"https://cdn.mathjax.org/mathjax/latest/MathJax.js?config=TeX-AMS_CHTML\"></script>")
                .append("</body>")
                .append("</html>").toString();

        Logger.d(html);

        mWebView.loadDataWithBaseURL("",
                html,
                "text/html",
                "UTF-8",
                "");
    }

    public void loadMarkdownFromAsset(String path, String cssPath)
    {
        loadMarkdown(Utils.getStringFromAssetFile(getContext().getAssets(), path), cssPath);
    }

    public interface Styles
    {
        String GITHUB = "file:///android_asset/css/github.css";
        String GITHUB_DARK = "file:///android_asset/css/github-dark.css";
    }

    public static class NodeRendererFactoryImpl implements NodeRendererFactory
    {
        @Override
        public NodeRenderer create(DataHolder options)
        {
            return new NodeRenderer()
            {
                @Override
                public Set<NodeRenderingHandler<?>> getNodeRenderingHandlers()
                {
                    HashSet<NodeRenderingHandler<?>> set = new HashSet<>();
                    set.add(new NodeRenderingHandler<>(Image.class, new CustomNodeRenderer<Image>()
                    {
                        @Override
                        public void render(Image node, NodeRendererContext context, HtmlWriter html)
                        {
                            if(!context.isDoNotRenderLinks())
                            {
                                String altText = new TextCollectingVisitor().collectAndGetText(node);

                                ResolvedLink resolvedLink = context.resolveLink(LinkType.IMAGE, node.getUrl().unescape(), null);
                                String url = resolvedLink.getUrl();

                                if(!node.getUrlContent().isEmpty())
                                {
                                    // reverse URL encoding of =, &
                                    String content = Escaping.percentEncodeUrl(node.getUrlContent()).replace("+", "%2B").replace("%3D", "=").replace("%26", "&amp;");
                                    url += content;
                                }

                                final int index = url.indexOf('@');

                                if(index >= 0)
                                {
                                    String[] dimensions = url.substring(index + 1, url.length()).split("\\|");
                                    url = url.substring(0, index);

                                    if(dimensions.length == 2)
                                    {
                                        String width = TextUtils.isEmpty(dimensions[0]) ? "auto" : dimensions[0];
                                        String height = TextUtils.isEmpty(dimensions[1]) ? "auto" : dimensions[1];
                                        html.attr("style", "width: " + width + "; height: " + height);
                                    }
                                }

                                html.attr("src", url);
                                html.attr("alt", altText);

                                if(node.getTitle().isNotNull())
                                {
                                    html.attr("title", node.getTitle().unescape());
                                }

                                html.srcPos(node.getChars()).withAttr(resolvedLink).tagVoid("img");
                            }
                        }
                    }));
                    return set;
                }
            };
        }


    }
}
