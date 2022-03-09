package main

import (
	"context"
	"encoding/json"
	"errors"
	"fmt"
	"io"
	"os"
	"strings"

	go_ipfs_api "github.com/ipfs/go-ipfs-api"
	files "github.com/ipfs/go-ipfs-files"
)

type object struct {
	Hash string
}

func main() {
	// Where your local node is running on localhost:5001
	sh := go_ipfs_api.NewShell("localhost:5001")
	cid, err := sh.Add(strings.NewReader("hello world!"), func(rb *go_ipfs_api.RequestBuilder) error {
		rb.Option("wrap-with-directory", true)
		return nil
	})
	if err != nil {
		fmt.Fprintf(os.Stderr, "error: %s", err)
		os.Exit(1)
	}
	fmt.Printf("added %s \n", cid)
	cid, err = sh.AddDir("./test1")
	if err != nil {
		fmt.Fprintf(os.Stderr, "error: %s", err)
		os.Exit(1)
	}
	fmt.Printf("added %s \n", cid)

	cid, err = sendTxt(sh, "are u ok?")
	if err != nil {
		fmt.Fprintf(os.Stderr, "error: %s", err)
		os.Exit(1)
	}
	fmt.Printf("added %s \n", cid)
}

func sendTxt(s *go_ipfs_api.Shell, txt string) (string, error) {
	fr := files.NewReaderFile(strings.NewReader(txt))
	slf := files.NewSliceDirectory([]files.DirEntry{files.FileEntry("3.txt", fr)})
	fileReader := files.NewMultiFileReader(slf, true)

	resp, err := s.Request("add").
		Option("wrap-with-directory", true).
		Body(fileReader).
		Send(context.Background())
	if err != nil {
		return "", nil
	}

	defer resp.Close()

	if resp.Error != nil {
		return "", resp.Error
	}

	dec := json.NewDecoder(resp.Output)
	var final string
	for {
		var out object
		err = dec.Decode(&out)
		if err != nil {
			if err == io.EOF {
				break
			}
			return "", err
		}
		final = out.Hash
	}

	if final == "" {
		return "", errors.New("no results received")
	}

	return final, nil
}
