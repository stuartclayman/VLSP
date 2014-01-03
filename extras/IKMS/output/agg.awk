BEGIN { OFS = "\t" }
{
  if ($1 != last) {
    if (NR > 1) print last, a
    last = $1
    a = $2
  } else a = a ", " $2
}
END { print last, a }
