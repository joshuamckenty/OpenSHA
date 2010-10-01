# this script removes old gmt plots. it can be run manually if needed, but is currently
# running every night at midnight as a cron job. it leaves directories that are only
# 6 or less hours old, so that if you make a map close to midnight, it won't get deleted
# until the next day


# the data directory
cd /usr/local/tomcat/default/webapps/OpenSHA/gmtData

# the buffer zone so that directories that have been modified within this time period
# won't get deleted.
mins=360 # 6 hours

# this find command find every directory that is exactly 1 level below the gmtData directory
# and hasn't been modified in at least $mins (see above) minutes, then calls 'rm -rfv DIR_NAME'
# on each one.
find -mindepth 1 -maxdepth 1 -type d -cmin +$mins -exec rm -rfv {} \;
