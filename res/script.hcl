# These are shortcuts so that there is less typing when entering code
# on the phone.
alias lcdui.alert	/alert
alias lcdui.canvas	/canvas
alias lcdui.choicegroup	/choice
alias lcdui.command	/cmd
alias lcdui.date	/date
alias lcdui.font	/font
alias lcdui.form	/form
alias lcdui.gauge	/gauge
alias lcdui.image	/img
alias lcdui.imageitem	/imgit
alias lcdui.list	/list
alias lcdui.spacer	/spc
alias lcdui.stringitem	/strit
alias lcdui.textbox	/txtbox
alias lcdui.textfield	/txt
alias lcdui.ticker	/ticker
# short name for file:///
proc /root {file} {
    return [append "file:///" $file]
}

# Capture puts output
rename puts DEBUG

proc puts {txt} {
[/alert -type confirmation -timeout forever \
     -title Alert -text "$txt" \
] setcurrent
}