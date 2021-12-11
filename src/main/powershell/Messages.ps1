$global:messages = Data {
ConvertFrom-StringData -StringData @'
    E001001 = The control statement is not closed correctly.
    E002001 = This open was duplicated, cannot be performed on a control statement that has already been opened.
    E002002 = Incorrect combination of control statements.
    E002003 = Close is invalid for non-nested control statements.
    E002004 = The number of header/footer lines exceeds the predefined total number of block lines.
    E002005 = Output is invalid for non-nested control statements.
    E002006 = The description control statement is not defined in the code control statement.
    E002007 = The assignment control statement is not defined in the code control statement.
    E002008 = The condition control statement is not defined in the code control statement.
    W002001 = The extended parameter value is invalid and will be ignored.
    E003001 = Invalid number of lines.
    E003002 = Cannot start output for a specified line, because that has already been output.
    E003003 = Output cannot be started for rows outbounds the transaction range that have already been started.
    E003004 = Cannot operation for a specified line, because that has already been output.
    E003005 = The number of lines specified for output is less than one.
    E004001 = The search starting point is unknown.
    E005001 = The specified a template file was not found.
    E005002 = The specified output path is not a directory.
    E005003 = The specified path is not a file or directory.
'@
}
