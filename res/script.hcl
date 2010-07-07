# These are shortcuts so that there is less typing when entering code
# on the phone.
alias lcdui.alert /alert
alias lcdui.canvas /canvas
alias lcdui.choicegroup /choice
alias lcdui.command /cmd
alias lcdui.date /date
alias lcdui.font /font
alias lcdui.form /form
alias lcdui.gauge /gauge
alias lcdui.image /img
alias lcdui.imageitem /imgit
alias lcdui.list /list
alias lcdui.spacer /spc
alias lcdui.stringitem /strit
alias lcdui.textbox /txtbox
alias lcdui.textfield /txt
alias lcdui.ticker /ticker

# short names for paths to root drives
proc c: {} {
    return "file:///c:/"
}

proc e: {} {
    return "file:///e:/"
}

# Capture puts output
rename puts DEBUG

# set :out ""
proc puts {txt} {
    global :out
    append $:out "$txt\n"
}

# Anonymous proc generator
set :ac 0
proc : {args body} {
    global :ac
    set :ac [1+ $:ac]
    set name ":anon:$:ac"
    proc $name $args $body
    return $name
}

