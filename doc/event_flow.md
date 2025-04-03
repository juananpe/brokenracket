# Use case: Book Court

# Event flow:

## Precondition

Member is logged in

## Main flow

1) Member provides desired booking date and court
2) System checks if date is within a month
3) System shows occupation for that date and court
4) Member chooses one of the free hours
5) System saves reservation as OCCUPIED including member, day, hour, court and rate (R1 or R2)
6) System informs that reservation has been successful


## Alternative flow

A.3) If date is later than a month System asks member to change it and gets back to step 1 B.4) If member doesn't find a suitable hour s/he can cancel and leave (END) or go back to step 1

