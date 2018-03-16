%%
% jdrasilmanual.cls
%%%%%%%%%%%%%%%%%%%
%
% The jdrasilmanual class is used to set the Jdrasil manual.
%

% extend article
\LoadClass{report}
\ProvidesClass{jdrasilmanual}

% math libraries
\RequirePackage{amsmath}
\RequirePackage{amssymb}
\RequirePackage{euler}

% Standard LuaTeX Packages
\RequirePackage{luatextra}
\RequirePackage{luatexbase}
\RequirePackage{luacode}

% Font Selection
\setmainfont{Alegreya}
\newfontfamily{\codefamily}{Source Code Pro}

%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
% TikZ and Colors
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

\RequirePackage{tikz}
\usetikzlibrary{graphs, graphdrawing}
\usegdlibrary{trees, force}

% define some colors - thanks to http://latexcolor.com
\definecolor{emerald}{rgb}{0.31, 0.78, 0.47}
\definecolor{coralred}{rgb}{1.0, 0.25, 0.25}

% colors used in the template
\colorlet{jdrasil.fg}{emerald!75!black}
\colorlet{jdrasil.bg}{emerald!75!black}
\colorlet{jdrasil.alert}{coralred!85!black}

%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
% Tables
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
\RequirePackage{booktabs}
\RequirePackage{longtable}

%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
% BibTeX
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
\bibliographystyle{plainurl}

%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
% Logo and Text
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

% Shameless Self-Promotion
\newcommand{\Jdrasil}{\emph{\color{jdrasil.fg}J}drasil}

% The logo command
\newcommand{\logo}[1]{%
\scalebox{#1}{
\begin{tikzpicture}

\draw[rounded corners] (0.05,0) .. controls (0.15,0.95) .. (0.05,1.9);
\draw[rounded corners] (0.95,0) .. controls (0.85,0.95) .. (0.95,1.9);
\draw[rounded corners] (-0.8,-.3) .. controls (0.25,0) .. (1.7,-.15);

\graph[no placement, empty nodes, 
nodes={draw, circle, inner sep=1.5}]{

% tree nodes
a[at={(0.2,0.2)}];
b[at={(0.25,0.6)}];
c[at={(0.75,0.75)}];
d[at={(0.6,0.3)}];
e[at={(0.4,1.1)}];
f[at={(0.75,1.5)}];
g[at={(0.3,1.75)}];

x1[at={(0.2,2)}];
%x2[at={(0.5,2.2)}];
x2[at={(0.5,2.3)}];
x3[at={(0.8,2)}];

% edges
a--b--c--a;
a--d--c;
b--e--c;
e--{f--c,g};
g -- {x1, x2, x3};
f -- x3;

% leafs
l1[at={(-.1,2.05)}];
l2[at={(-.05,2.25)}];
l3[at={(.15,2.3)}];
x1 -- {l1,l2,l3};

%l4[at={(.3,2.55)}];
%l5[at={(.5,2.65)}];
%l6[at={(.7,2.55)}];
%x2 -- {l4,l5,l6};

l4[at={(.3,2.7)}];
l5[at={(.5,2.8)}];
l6[at={(.7,2.7)}];
l10[at={(.21,2.525)}];
l11[at={(.79,2.525)}];
x2 -- {l4,l5,l6, l10, l11};


l7[at={(1+.1,2.05)}];
l8[at={(1+.05,2.25)}];
l9[at={(1-.15,2.3)}];
x3 -- {l7,l8,l9};

};
\end{tikzpicture}
}
}

%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
% Page Layout
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

% packages for pagelayout
\RequirePackage{geometry}
\RequirePackage{titlesec}
\RequirePackage{titling}

% default page stuff
\parskip=\medskipamount
\parindent=0pt
\marginparwidth=3.5cm

% geometry stuff
\geometry{hmargin={2.5cm, 5cm}}
\geometry{vmargin={3cm, 5cm}}

% begin of the document
\AtBeginDocument{%
  \maketitle
  \cleardoublepage
  % legal information
  ~\vfill
Copyright (c) 2016-\the\year, \theauthor

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
associated documentation files (the "Software"), to deal in the Software without restriction,
including without limitation the rights to use, copy, modify, merge, publish, distribute,
sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or
substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT
OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
  \clearpage
  \pagestyle{plain}
}
\AtEndDocument{%
}


% title page
\makeatletter
\renewcommand{\maketitle}{%
\pagestyle{empty}
\newgeometry{ left = 2cm, right = 2cm, top = 2cm, bottom = 2cm}

~\vspace{2.5cm}

\begin{center}
\logo{3}
\end{center}

\vfill
{\fontsize{62pt}{62pt}\selectfont \thetitle}

\vspace{0.5cm}
{\fontsize{32pt}{32pt}\selectfont For Version: {\color{jdrasil.fg}{\theversion}}}

\begin{flushright}
\vspace{3cm}
Compiled: \today{}
\end{flushright}
\clearpage
\restoregeometry
}
\makeatother

% meta data
\title{\emph{\textcolor{jdrasil.fg}{J}}drasil Manual}
\newcommand{\theversion}{1}
\newcommand{\setversion}[1]{\renewcommand{\theversion}{#1}}
\date{\today}

% part layout
\titleclass{\part}{top}
\titlespacing*{\part}{0pt}{100pt}{25pt} % left above below
\titleformat{\part}
[display] % shape
{\Huge} % format
{Part~\thepart:} % label
{0pt} % separation
{\color{jdrasil.fg}} % before
[] % after

% chapter layout
\titleclass{\chapter}{top}
\titlespacing*{\chapter}{0pt}{25pt}{20pt} % left above below
\titleformat{\chapter}
[block] % shape
{\huge} % format
{Chapter~\thechapter:} % label
{10pt} % separation
{\color{jdrasil.fg}} % before
[] % after

% section layout
\titleclass{\section}{straight}
\titleformat{\section}
[block] % shape
{\Large} % format
{\thesection} % label
{10pt} % separation
{} % before
[] % after

% subsection layout
\titleclass{\subsection}{straight}
\titleformat{\subsection}
[block] % shape
{\large} % format
{\thesubsection} % label
{10pt} % separation
{} % before
[] % after

%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
% Code
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

% display code fragments
\RequirePackage{listings}
%\lstset{basicstyle=\codefamily}
\lstset{% general command to set parameter(s) 
  basicstyle=\small\codefamily, % print whole listing small
  keywordstyle=\color{jdrasil.fg},
  identifierstyle=, 
  commentstyle=\color{jdrasil.fg}, 
  stringstyle=\ttfamily, 
  showstringspaces=false
}

\lstdefinelanguage{BibTeX}
  {keywords={%
      @article,@book,@collectedbook,@conference,@electronic,@ieeetranbstctl,%
      @inbook,@incollectedbook,@incollection,@injournal,@inproceedings,%
      @manual,@mastersthesis,@misc,@patent,@periodical,@phdthesis,@preamble,%
      @proceedings,@standard,@string,@techreport,@unpublished%
      },
   comment=[l][\itshape]{@comment},
   sensitive=false,
  }

\newcommand{\codeWithOutput}[2]{%
  \quad\lstinline[language=Java]{#1}
  \quad\tikz[baseline={([yshift=-0.5ex](0,0))}]{\draw[->, >=stealth,
    densely dashed] (0,0) -- (1,0);}\quad
  \lstinline[language=Java]{#2}
}

% load hyperref as last
\RequirePackage{hyperref}

% Class definitions
\newcommand\Class[1]{%
  \mathchoice%
  {\text{\normalfont\fontsize{9pt}{10pt}\selectfont$\mathrm{#1}$}}%
  {\text{\normalfont\fontsize{9pt}{10pt}\selectfont$\mathrm{#1}$}}%
  {\text{\normalfont$\mathrm{#1}$}}%
  {\text{\normalfont$\mathrm{#1}$}}%
}

% Prefix for parameterized classes
\newcommand\Para{\mathrm{para\text-}}

% Language definitions
\newcommand{\Lang}[1]{%
  \ifmmode{%
    \text{\normalfont\textsc{#1}}%
  }%
  \else
  {\normalfont\textsc{#1}}%
  \fi}

% Display the name of a Java class
\newcommand{\JClass}[1]{%
  \lstinline[language=Java]!#1!%
}

% Display the name of a Java method
\newcommand{\JMethod}[1]{%
  \lstinline[language=Java]!#1!
}

% Display the name of a file
\newcommand{\file}[1]{%
  \lstinline[]!#1!
}

% Display a note in margin par
\newcommand{\note}[1]{%
  \marginpar{%
    \small%
    \flushleft%
    \textcolor{jdrasil.fg}{Note:} #1
  }
}

% Display a detail for interested reader in margin par
\newcommand{\detail}[1]{%
  \marginpar{%
    \small%
    \flushleft%
    \textcolor{jdrasil.fg}{For the interested reader:} #1
  }
}


%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
% TODOs
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

% mange if todos should be displayed or not
\newif\iftodos
\todostrue

% Display a todo in margin par
\newcommand{\todo}[1]{%
  \iftodos
  \marginpar{%
    \small%
    \flushleft%
    \textcolor{jdrasil.alert}{ToDo:} #1
  }
  \fi
}

%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
% JavaParser
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

% load the parser
\luaexec{ JavaParser = require("JavaParser") }

% parse the given Java source code file and include its doc
\newcommand{\includeJavaDoc}[1]{%
  \luaexec{ JavaParser.parse("#1") }
}