package main

import (
	"context"
	"encoding/json"
	"errors"
	"fmt"
	"io"
	"strings"

	"github.com/beego/beego/v2/client/httplib"
	go_ipfs_api "github.com/ipfs/go-ipfs-api"
	files "github.com/ipfs/go-ipfs-files"
)

type object struct {
	Hash string
}

//  API Key: b6c7a19fc50650367b50
//  API Secret: c2bbf4fe0909a32b1aebce8cb3ad306ff684ec2a9268de946351e533389464ff
//  JWT: eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VySW5mb3JtYXRpb24iOnsiaWQiOiJjZWJlZWI0YS03ZDNhLTRkMmEtYmFkNS1kMDZlZjI3MjcwNjkiLCJlbWFpbCI6Imh5dDU5MjZAMTYzLmNvbSIsImVtYWlsX3ZlcmlmaWVkIjp0cnVlLCJwaW5fcG9saWN5Ijp7InJlZ2lvbnMiOlt7ImlkIjoiRlJBMSIsImRlc2lyZWRSZXBsaWNhdGlvbkNvdW50IjoxfV0sInZlcnNpb24iOjF9LCJtZmFfZW5hYmxlZCI6ZmFsc2V9LCJhdXRoZW50aWNhdGlvblR5cGUiOiJzY29wZWRLZXkiLCJzY29wZWRLZXlLZXkiOiJiNmM3YTE5ZmM1MDY1MDM2N2I1MCIsInNjb3BlZEtleVNlY3JldCI6ImMyYmJmNGZlMDkwOWEzMmIxYWViY2U4Y2IzYWQzMDZmZjY4NGVjMmE5MjY4ZGU5NDYzNTFlNTMzMzg5NDY0ZmYiLCJpYXQiOjE2NDc3OTYxNDF9.0ykNdUtx6kyxRRjDkyRIJaZ3jbfgO46UYPztVUPh-_4
func main() {
	req := httplib.Get("https://api.web3.storage/pins")
	req.Header("Accept", "*/*")
	req.Header("Authorization", "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJkaWQ6ZXRocjoweDRCQ0IyQkExRjM3NDBhZjk2NjFhNTVjNTJGQzM4RDVCZTI2NGQ5NGQiLCJpc3MiOiJ3ZWIzLXN0b3JhZ2UiLCJpYXQiOjE2NDc3OTM0NjgwNjUsIm5hbWUiOiJlYWN0YWxrIn0.ZEvsmTKIXQd-Abqk0eU4xy3FWExDWle0p7sWQedcjts")

	res, err := req.String()
	if err != nil {
		fmt.Println(err.Error())
		return
	}
	fmt.Println(res)

	// fr := files.NewReaderFile(strings.NewReader("are u ok ?"))
	// slf := files.NewSliceDirectory([]files.DirEntry{files.FileEntry("", fr)})
	// fileReader := files.NewMultiFileReader(slf, true)

	// sh := go_ipfs_api.NewShell("localhost:5001")
	// resp, err := sh.Request("files").
	// 	Arguments("write").
	// 	Arguments("QmUNLLsPACCz1vLxQVkXqqLX5R1X345qqfHbsf67hvA3Nn/areuok.txt").
	// 	Option("create", true).
	// 	Body(fileReader).
	// 	Send(context.Background())

	// if err != nil {
	// 	fmt.Println(err.Error())
	// 	return
	// }

	// defer resp.Close()

	// if resp.Error != nil {
	// 	fmt.Println(resp.Error.Error())
	// 	return
	// }

	// buf := bufio.NewScanner(resp.Output)
	// for buf.Scan() {
	// 	fmt.Println(buf.Text())
	// }

	// dec := json.NewDecoder(resp.Output)
	// var final string
	// for {
	// 	var out object
	// 	err = dec.Decode(&out)
	// 	if err != nil {
	// 		if err == io.EOF {
	// 			break
	// 		}
	// 		fmt.Println(err.Error())
	// 		return
	// 	}
	// 	final = out.Hash
	// }

	// fmt.Println("final " + final)

	// sh := go_ipfs_api.NewShell("localhost:5001")
	// cid, err := sh.Add(strings.NewReader("hello world!"), func(rb *go_ipfs_api.RequestBuilder) error {
	// 	rb.Option("wrap-with-directory", true)
	// 	return nil
	// })
	// if err != nil {
	// 	fmt.Fprintf(os.Stderr, "error: %s", err)
	// 	os.Exit(1)
	// }
	// fmt.Printf("added %s \n", cid)
	// cid, err = sh.AddDir("./test1")
	// if err != nil {
	// 	fmt.Fprintf(os.Stderr, "error: %s", err)
	// 	os.Exit(1)
	// }
	// fmt.Printf("added %s \n", cid)

	// cid, err = sendTxt(sh, "are u ok?")
	// if err != nil {
	// 	fmt.Fprintf(os.Stderr, "error: %s", err)
	// 	os.Exit(1)
	// }
	// fmt.Printf("added %s \n", cid)
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
