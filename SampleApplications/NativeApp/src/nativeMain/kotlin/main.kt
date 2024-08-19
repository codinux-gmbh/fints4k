import com.github.ajalt.clikt.core.subcommands
import commands.TransferMoneyCommand
import commands.fints4kCommandLineInterface

fun main(args: Array<String>) {
  fints4kCommandLineInterface().subcommands(TransferMoneyCommand()).main(args)
}