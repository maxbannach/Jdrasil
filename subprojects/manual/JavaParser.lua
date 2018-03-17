---
-- A Lua script to parse Java source code files and create TeX representation of it.
--
local JavaParser = {}

---
-- Trim a given string by removing a sequence of spaces followed by a eventual sequence of *.
--
local function trim(s)   
   return (s:gsub("^[%s]*%**(.-)", "%1"))
end

---
-- Print the signature of the given Java function to TeX.
--
function JavaParser.printFunctionName(func)
   if func:find("\n") then
      tex.print("\\begin{lstlisting}[language=Java]")
      local i = 0
      for line in func:gmatch("([^\n]+)") do
	 line = trim(line)
	 if i > 0 then line = "  "..line end
	 tex.print(line)
	 i = i + 1
      end
      tex.print("\\end{lstlisting}")
   else
      tex.sprint("\\lstinline[language=Java]|"..trim(func).."|")
   end
end

---
-- Print the comment of the given Java function to TeX.
--
function JavaParser.printFunctionComment(comment, skip)

   -- make sure every comment has an end line
   if not comment:find("\n") then comment = "\n"..comment.."\n" end

   tex.print("")
   tex.sprint("\\begingroup\\leftskip="..skip)--0.5cm
   local typedParam = false
   for line in comment:gmatch("(.-)\n") do
      local t = trim(line):gsub("%@see%s(%w*)", "\\textcolor{jdrasil.fg}{\\small\\codefamily %1}")
      
      -- check if parameter line
      local x,y = t:match("%@param%s(.-)%s(.*)")

      -- check return value line
      local z = t:match("%@return%s(.*)")

      local author = t:match("%@author")
      
      -- check for throws
      local exception,e = t:match("%@throws%s(.-)%s(.*)")
      
      -- actually print to TeX
      if (author) then
	 -- skip
      elseif (x) then
	 if typedParam then tex.sprint("\\\\") end
	 tex.print("\\emph{Parameter "..x..":} "..y)	 
	 typedParam = true
      elseif (z) then
	 if typedParam then tex.sprint("\\\\") end
	 tex.print("\\emph{Return Value: } "..z)
	 typedParam = true
      elseif (exception) then
	 if typedParam then tex.sprint("\\\\") end
	 tex.print("\\emph{Throws "..exception..": } "..e)
	 typedParam = true
      else
	 tex.print(t)	 
      end
   end
   tex.print("")
   tex.sprint("\\endgroup")
end

---
-- Parse the JavaDoc out of a Java source code file.
--
function JavaParser.parse(path)
   local file = io.open(path, "r")

   local content = {}
   for line in file:lines() do table.insert(content, line) end

   local s = table.concat(content, "\n")
   for comment,func in s:gmatch("/%*%*(.-)%*/\n(.-)[{;]") do
      tex.sprint("\\goodbreak{%")
      if not func:match(" class ") and not func:match(" interface ") then
	 JavaParser.printFunctionName(func)
	 JavaParser.printFunctionComment(comment, "0.5cm")
      else
	 JavaParser.printFunctionComment(comment, "0cm")
      end
      tex.sprint("}")
   end

   file:close()
end
   

-- done
return JavaParser
