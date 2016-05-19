#!/bin/bash

cmd=$1
if [ -z $cmd ]; then
  exit 100 # Must have a command
fi
shift

# If enviorment is Mac OS, we enable test mode for development.
# Mac OS has no command 'useradd' and 'usermod'.
MODE="RUN"
if [ $(uname -s) == 'Darwin' ]; then
	MODE="TEST"
fi


prepare_params(){
	host=$1
	shift

	if [ $cmd != 'addGroup' ] && [ $cmd != 'delGroup' ]; then
		user=$1
		if [ -z $user ]; then
		  exit 101 # Must have a specific user
		fi
		shift
	fi

	groups=()
	while [ ! -z $1 ]
	do
	  next=${#groups[*]}
	  groups[$next]=$1
	  shift
	done

	if [[ $cmd =~ .*Group$ ]] && [ ${#groups[*]} -eq 0 ]
	then
	  exit 102 # Must have specific groups
	fi
}


add_user(){
  prepare_params $@
  ssh $host sudo useradd $user -M -s /sbin/nologin

  if [ $? -ne 0 ]; then
    exit 103 # Occur exception
  fi
}

add_group(){
	prepare_params $@
	g=${groups[0]}
	ssh $host sudo groupadd $g
	if [ $? -ne 0 ]; then
		exit 103 #Occur exception
	fi
}

append_group(){
  prepare_params $@
  local Gargs=""
  for g in ${groups[*]}; do
    if [ -z $Gargs ]; then
      Gargs=$g
    else
      Gargs=$Gargs','$g
    fi  
  done
  ssh $host sudo usermod -a -G $Gargs $user

  if [ $? -ne 0 ]; then
    exit 103 # Occur exception
  fi
}

find_user(){
  prepare_params $@
  res=$(ssh $host id $user)

  if [ $? -ne 0 ]; then
    exit 103 # Occur exception
  fi

  groups=(`echo $res | grep -o 'groups.*$' | grep -oE '\([\.a-zA-Z1-9_]+\)' | grep -oE '[\.a-zA-Z0-9_]+'`)
	
  # '$</>' is a mark to protect valid output.
  echo -n '$<'${groups[*]}'>$'

  if [ $? -ne 0 ]; then
    exit 104 # Failed to output groups information.
  fi
}

delete_user(){
  prepare_params $@
  ssh $host sudo userdel -f $user
  
  if [ $? -ne 0 ]; then
    exit 103 # Occur exception
  fi
}

delete_group(){
  prepare_params $@
  g=${groups[0]}
  ssh $host sudo groupdel $g
  
  if [ $? -ne 0 ]; then
    exit 103 # Occur exception
  fi
}

# Simulate linux environment
if [ $MODE == 'TEST' ]; then
  	ILLEGAL_USER='TEST_U_ILLEGAL'
	ILLEGAL_GROUP="TEST_G_ILLEGAL"

	case $cmd in
		'addUser')
		    prepare_params $@
			if [ $user == $ILLEGAL_USER ]; then
				echo "useradd: user '$ILLEGAL_USER' already exists" >&2
				exit 103
			fi
		;;
		'delUser')
		    prepare_params $@
			if [ $user == $ILLEGAL_USER ]; then
				echo "userdel: user '$ILLEGAL_USER' doesn't exist" >&2
				exit 103
			fi
		;;
		'addGroup')
		    prepare_params $@
			if [ ${groups[0]} == $ILLEGAL_GROUP ]; then
				echo "groupadd: group '$ILLEGAL_GROUP' already exists" >&2
				exit 103
			fi
		;;
		'delGroup')
		    prepare_params $@
			if [ ${groups[0]} == $ILLEGAL_GROUP ]; then
				echo "groupdel: group '$ILLEGAL_GROUP' doesn't exist" >&2
				exit 103
			fi
		;;
		'appendGroup')
		    prepare_params $@
			if [ $user == $ILLEGAL_USER ]; then
				echo "usermod: user '$ILLEGAL_USER' does not exist" >&2
				exit 103
			fi
			for g in ${groups[*]}; do
				if [ $g == $ILLEGAL_GROUP ]; then
					echo "usermod: group '$ILLEGAL_GROUP' does not exist" >&2
					exit 103
				fi
			done
		;;
		'findUser')
			find_user $@
		;;
		*)
  			echo "Unsupported command '"$cmd"'"
			exit 105 # Unsupported command
		;;
  esac
	exit 0
fi

case $cmd in
  'addUser')
	add_user $@
  ;;
  'delUser')
	delete_user $@
  ;;
  'addGroup')
	add_group $@
  ;;
  'delGroup')
	delete_group $@
  ;;
  'appendGroup')
	append_group $@
  ;;
  'findUser')
	find_user $@
  ;;
  *)
  	echo "Unsupported command '"$cmd"'"
	exit 105 # Unsupported command
  ;;
esac
